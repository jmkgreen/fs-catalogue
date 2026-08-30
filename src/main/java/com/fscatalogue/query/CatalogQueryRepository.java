package com.fscatalogue.query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CatalogQueryRepository {
    private final Connection connection;

    public CatalogQueryRepository(Connection connection) {
        this.connection = connection;
    }

    public List<CatalogEntry> find(String name, String nameLike, Long size, String rootName) throws SQLException {
        StringBuilder sql = new StringBuilder("""
                SELECT r.name, r.path, e.relative_path, e.filename, e.type,
                       e.size_bytes, e.modified_time, e.extension
                FROM entries e
                JOIN roots r ON r.id = e.root_id
                WHERE 1 = 1
                """);
        List<Binder> binders = new ArrayList<>();
        if (name != null) {
            sql.append(" AND e.filename_normalized = ?");
            binders.add(statement -> statement.setString(1, name.toLowerCase(Locale.ROOT)));
        }
        if (nameLike != null) {
            sql.append(" AND e.filename_normalized LIKE ? ESCAPE '\\'");
            binders.add(statement -> statement.setString(1, wildcardToSqlLike(nameLike.toLowerCase(Locale.ROOT))));
        }
        if (size != null) {
            sql.append(" AND e.size_bytes = ?");
            binders.add(statement -> statement.setLong(1, size));
        }
        if (rootName != null) {
            sql.append(" AND r.name = ?");
            binders.add(statement -> statement.setString(1, rootName));
        }
        sql.append(" ORDER BY r.name, e.relative_path");
        return query(sql.toString(), binders);
    }

    public List<CatalogEntry> listChildren(String mountedPath) throws SQLException {
        String sql = """
                SELECT child_root.name, child_root.path, e.relative_path, e.filename, e.type,
                       e.size_bytes, e.modified_time, e.extension
                FROM entries e
                JOIN roots child_root ON child_root.id = e.root_id
                JOIN roots requested_root
                  ON requested_root.id = e.root_id
                 AND (? = requested_root.path
                      OR ? LIKE requested_root.path || '/%')
                WHERE e.relative_path <> ''
                  AND e.parent_relative_path = CASE
                    WHEN ? = requested_root.path THEN ''
                    ELSE substr(?, length(requested_root.path) + 2)
                END
                ORDER BY e.type, e.filename_normalized
                """;
        return query(sql, List.of(
                statement -> statement.setString(1, mountedPath),
                statement -> statement.setString(1, mountedPath),
                statement -> statement.setString(1, mountedPath),
                statement -> statement.setString(1, mountedPath)));
    }

    public CatalogStats stats() throws SQLException {
        long roots = scalar("SELECT COUNT(*) FROM roots");
        long directories = scalar("SELECT COUNT(*) FROM entries WHERE type = 'directory'");
        long files = scalar("SELECT COUNT(*) FROM entries WHERE type = 'file'");
        long bytes = scalar("SELECT COALESCE(SUM(size_bytes), 0) FROM entries WHERE type = 'file'");
        return new CatalogStats(roots, directories, files, bytes);
    }

    private long scalar(String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSet.getLong(1);
        }
    }

    private List<CatalogEntry> query(String sql, List<Binder> binders) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < binders.size(); i++) {
                int index = i + 1;
                binders.get(i).bind(new IndexedStatement(statement, index));
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<CatalogEntry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    long size = resultSet.getLong("size_bytes");
                    entries.add(new CatalogEntry(
                            resultSet.getString("name"),
                            resultSet.getString("path"),
                            resultSet.getString("relative_path"),
                            resultSet.getString("filename"),
                            resultSet.getString("type"),
                            resultSet.wasNull() ? null : size,
                            resultSet.getString("modified_time"),
                            resultSet.getString("extension")));
                }
                return entries;
            }
        }
    }

    private static String wildcardToSqlLike(String pattern) {
        StringBuilder sqlLike = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char character = pattern.charAt(i);
            switch (character) {
                case '*' -> sqlLike.append('%');
                case '?' -> sqlLike.append('_');
                case '%', '_', '\\' -> sqlLike.append('\\').append(character);
                default -> sqlLike.append(character);
            }
        }
        return sqlLike.toString();
    }

    @FunctionalInterface
    private interface Binder {
        void bind(IndexedStatement statement) throws SQLException;
    }

    private record IndexedStatement(PreparedStatement statement, int index) {
        void setString(int ignoredIndex, String value) throws SQLException {
            statement.setString(index, value);
        }

        void setLong(int ignoredIndex, long value) throws SQLException {
            statement.setLong(index, value);
        }
    }
}

