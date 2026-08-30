package com.fscatalogue.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

final class CatalogAppCliTest {
    @TempDir
    private Path tempDir;

    @Test
    void scansAndQueriesThroughCli() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Files.writeString(root.resolve("Foo.txt"), "hello");
        Path database = tempDir.resolve("catalogue.db");
        Path config = writeConfig(root, database);

        assertEquals(0, execute("--db", database.toString(), "--config", config.toString(), "scan"));

        String findOutput = captureOutput(
                "--db", database.toString(),
                "--config", config.toString(),
                "find", "--name", "foo.txt");
        assertTrue(findOutput.contains("Foo.txt"));

        String jsonOutput = captureOutput(
                "--db", database.toString(),
                "find", "--name-like", "*.txt", "--json");
        assertTrue(jsonOutput.contains("\"filename\":\"Foo.txt\""));

        String statsOutput = captureOutput("--db", database.toString(), "stats", "--json");
        assertTrue(statsOutput.contains("\"files\":1"));

        String lsOutput = captureOutput("--db", database.toString(), "ls", root.toString());
        assertTrue(lsOutput.contains("Foo.txt"));
    }

    @Test
    void showsUsageWhenNoSubcommandIsProvided() {
        String output = captureOutput();

        assertTrue(output.contains("Searchable SQLite catalogue"));
    }

    @Test
    void returnsErrorWhenFindHasNoCriteria() {
        int exitCode = execute("--db", tempDir.resolve("missing.db").toString(), "find");

        assertEquals(1, exitCode);
    }

    @Test
    void returnsErrorForUnknownScanRoot() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Path database = tempDir.resolve("catalogue.db");
        Path config = writeConfig(root, database);

        int exitCode = execute(
                "--db", database.toString(),
                "--config", config.toString(),
                "scan", "--root", "missing");

        assertEquals(1, exitCode);
    }

    private Path writeConfig(Path root, Path database) throws Exception {
        Path config = tempDir.resolve("catalogue.yml");
        Files.writeString(config, """
                database: %s
                roots:
                  media: %s
                """.formatted(database, root));
        return config;
    }

    private static String captureOutput(String... args) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream replacement = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(replacement);
            execute(args);
            return bytes.toString(StandardCharsets.UTF_8);
        } finally {
            System.setOut(originalOut);
        }
    }

    private static int execute(String... args) {
        return new CommandLine(new CatalogApp()).execute(args);
    }
}
