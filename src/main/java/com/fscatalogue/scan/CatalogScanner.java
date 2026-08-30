package com.fscatalogue.scan;

import com.fscatalogue.config.CatalogConfig;
import com.fscatalogue.db.CatalogDatabase;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

public final class CatalogScanner {
    private final CatalogDatabase database;

    public CatalogScanner(CatalogDatabase database) {
        this.database = database;
    }

    public ScanSummary scan(CatalogConfig config, String selectedRoot) throws IOException, SQLException {
        ScanSummary total = new ScanSummary(0, 0, 0);
        for (Map.Entry<String, Path> root : config.roots().entrySet()) {
            if (selectedRoot != null && !selectedRoot.equals(root.getKey())) {
                continue;
            }
            total = total.plus(scanRoot(root.getKey(), root.getValue()));
        }
        if (selectedRoot != null && total.rootsScanned() == 0) {
            throw new IllegalArgumentException("Unknown root: " + selectedRoot);
        }
        return total;
    }

    private ScanSummary scanRoot(String name, Path rootPath) throws IOException, SQLException {
        Path normalizedRoot = rootPath.toAbsolutePath().normalize();
        if (!Files.isDirectory(normalizedRoot)) {
            throw new IOException("Root is not a directory: " + normalizedRoot);
        }

        Connection connection = database.connection();
        boolean previousAutoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try {
            long rootId = upsertRoot(connection, name, normalizedRoot);
            long scanRunId = startScanRun(connection, rootId);
            CountingVisitor visitor = new CountingVisitor(connection, rootId, scanRunId, normalizedRoot);
            Files.walkFileTree(normalizedRoot, visitor);
            visitor.close();
            deleteMissingEntries(connection, rootId, scanRunId);
            completeScanRun(connection, scanRunId);
            connection.commit();
            return new ScanSummary(1, visitor.directoriesSeen(), visitor.filesSeen());
        } catch (IOException | SQLException | RuntimeException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(previousAutoCommit);
        }
    }

    private static long upsertRoot(Connection connection, String name, Path path) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO roots(name, path, updated_at)
                VALUES (?, ?, CURRENT_TIMESTAMP)
                ON CONFLICT(name) DO UPDATE SET path = excluded.path, updated_at = CURRENT_TIMESTAMP
                RETURNING id
                """)) {
            statement.setString(1, name);
            statement.setString(2, path.toString());
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.getLong(1);
            }
        }
    }

    private static long startScanRun(Connection connection, long rootId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "INSERT INTO scan_runs(root_id) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, rootId);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                return keys.getLong(1);
            }
        }
    }

    private static void completeScanRun(Connection connection, long scanRunId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE scan_runs SET completed_at = CURRENT_TIMESTAMP WHERE id = ?")) {
            statement.setLong(1, scanRunId);
            statement.executeUpdate();
        }
    }

    private static void deleteMissingEntries(Connection connection, long rootId, long scanRunId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "DELETE FROM entries WHERE root_id = ? AND last_seen_run_id <> ?")) {
            statement.setLong(1, rootId);
            statement.setLong(2, scanRunId);
            statement.executeUpdate();
        }
    }

    private static final class CountingVisitor extends SimpleFileVisitor<Path> {
        private final Connection connection;
        private final long rootId;
        private final long scanRunId;
        private final Path rootPath;
        private final PreparedStatement upsertEntry;
        private long directoriesSeen;
        private long filesSeen;

        CountingVisitor(Connection connection, long rootId, long scanRunId, Path rootPath) throws SQLException {
            this.connection = connection;
            this.rootId = rootId;
            this.scanRunId = scanRunId;
            this.rootPath = rootPath;
            this.upsertEntry = connection.prepareStatement("""
                    INSERT INTO entries(
                        root_id,
                        relative_path,
                        parent_relative_path,
                        filename,
                        filename_normalized,
                        type,
                        size_bytes,
                        modified_time,
                        extension,
                        last_seen_run_id
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT(root_id, relative_path) DO UPDATE SET
                        parent_relative_path = excluded.parent_relative_path,
                        filename = excluded.filename,
                        filename_normalized = excluded.filename_normalized,
                        type = excluded.type,
                        size_bytes = excluded.size_bytes,
                        modified_time = excluded.modified_time,
                        extension = excluded.extension,
                        last_seen_run_id = excluded.last_seen_run_id
                    """);
        }

        long directoriesSeen() {
            return directoriesSeen;
        }

        long filesSeen() {
            return filesSeen;
        }

        void close() throws SQLException {
            upsertEntry.close();
        }

        @Override
        public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException {
            if (!directory.equals(rootPath) && shouldSkip(directory)) {
                return FileVisitResult.SKIP_SUBTREE;
            }
            try {
                upsert(directory, "directory", null, attributes.lastModifiedTime().toInstant());
                directoriesSeen++;
            } catch (SQLException ex) {
                throw new IOException(ex);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            if (shouldSkip(file) || !attributes.isRegularFile()) {
                return FileVisitResult.CONTINUE;
            }
            try {
                upsert(file, "file", attributes.size(), attributes.lastModifiedTime().toInstant());
                filesSeen++;
            } catch (SQLException ex) {
                throw new IOException(ex);
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) {
            return FileVisitResult.CONTINUE;
        }

        private boolean shouldSkip(Path path) throws IOException {
            Path filename = path.getFileName();
            if (filename != null && filename.toString().startsWith(".")) {
                return true;
            }
            return Files.isHidden(path);
        }

        private void upsert(Path path, String type, Long sizeBytes, Instant modifiedTime) throws SQLException {
            String relativePath = normalize(rootPath.relativize(path));
            String parentRelativePath = parentOf(relativePath);
            String filename = path.equals(rootPath) ? "" : path.getFileName().toString();
            String extension = extensionOf(filename);
            upsertEntry.setLong(1, rootId);
            upsertEntry.setString(2, relativePath);
            upsertEntry.setString(3, parentRelativePath);
            upsertEntry.setString(4, filename);
            upsertEntry.setString(5, filename.toLowerCase(Locale.ROOT));
            upsertEntry.setString(6, type);
            if (sizeBytes == null) {
                upsertEntry.setNull(7, java.sql.Types.BIGINT);
            } else {
                upsertEntry.setLong(7, sizeBytes);
            }
            upsertEntry.setString(8, modifiedTime.toString());
            upsertEntry.setString(9, extension);
            upsertEntry.setLong(10, scanRunId);
            upsertEntry.executeUpdate();
        }

        private static String normalize(Path path) {
            String normalized = path.toString().replace('\\', '/');
            return ".".equals(normalized) ? "" : normalized;
        }

        private static String parentOf(String relativePath) {
            int index = relativePath.lastIndexOf('/');
            if (index < 0) {
                return "";
            }
            return relativePath.substring(0, index);
        }

        private static String extensionOf(String filename) {
            int index = filename.lastIndexOf('.');
            if (index <= 0 || index == filename.length() - 1) {
                return "";
            }
            return filename.substring(index + 1).toLowerCase(Locale.ROOT);
        }
    }
}

