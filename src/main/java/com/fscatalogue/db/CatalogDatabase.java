package com.fscatalogue.db;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class CatalogDatabase implements AutoCloseable {
    private static final int SCHEMA_VERSION = 1;

    private final Connection connection;

    private CatalogDatabase(Connection connection) {
        this.connection = connection;
    }

    public static CatalogDatabase open(Path databasePath) throws SQLException, IOException {
        Path parent = databasePath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        CatalogDatabase database = new CatalogDatabase(connection);
        database.configure();
        database.migrate();
        return database;
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() throws SQLException {
        connection.close();
    }

    private void configure() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA journal_mode = WAL");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
    }

    private void migrate() throws SQLException {
        int currentVersion;
        try (Statement statement = connection.createStatement()) {
            currentVersion = statement.executeQuery("PRAGMA user_version").getInt(1);
        }
        if (currentVersion > SCHEMA_VERSION) {
            throw new SQLException("Database schema is newer than this application understands.");
        }
        if (currentVersion == 0) {
            createVersionOneSchema();
        }
    }

    private void createVersionOneSchema() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE roots (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL UNIQUE,
                        path TEXT NOT NULL,
                        created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            statement.execute("""
                    CREATE TABLE scan_runs (
                        id INTEGER PRIMARY KEY,
                        started_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        completed_at TEXT,
                        root_id INTEGER NOT NULL REFERENCES roots(id) ON DELETE CASCADE
                    )
                    """);
            statement.execute("""
                    CREATE TABLE entries (
                        root_id INTEGER NOT NULL REFERENCES roots(id) ON DELETE CASCADE,
                        relative_path TEXT NOT NULL,
                        parent_relative_path TEXT NOT NULL,
                        filename TEXT NOT NULL,
                        filename_normalized TEXT NOT NULL,
                        type TEXT NOT NULL CHECK (type IN ('directory', 'file')),
                        size_bytes INTEGER,
                        modified_time TEXT NOT NULL,
                        extension TEXT NOT NULL,
                        last_seen_run_id INTEGER NOT NULL REFERENCES scan_runs(id) ON DELETE CASCADE,
                        PRIMARY KEY (root_id, relative_path)
                    )
                    """);
            statement.execute("CREATE INDEX entries_parent_idx ON entries(root_id, parent_relative_path)");
            statement.execute("CREATE INDEX entries_filename_idx ON entries(filename_normalized)");
            statement.execute("CREATE INDEX entries_filename_size_idx ON entries(filename_normalized, size_bytes)");
            statement.execute("CREATE INDEX entries_size_idx ON entries(size_bytes)");
            statement.execute("CREATE INDEX entries_extension_idx ON entries(extension)");
            statement.execute("PRAGMA user_version = 1");
        }
    }
}
