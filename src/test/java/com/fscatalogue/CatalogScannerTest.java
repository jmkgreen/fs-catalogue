package com.fscatalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fscatalogue.config.CatalogConfig;
import com.fscatalogue.db.CatalogDatabase;
import com.fscatalogue.query.CatalogEntry;
import com.fscatalogue.query.CatalogQueryRepository;
import com.fscatalogue.query.CatalogStats;
import com.fscatalogue.scan.CatalogScanner;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CatalogScannerTest {
    @TempDir
    private Path tempDir;

    @Test
    void scansFilesAndFindsNamesCaseInsensitively() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Path movie = Files.writeString(root.resolve("Foo.MP4"), "sample-data");
        Path database = tempDir.resolve("catalogue.db");

        scan(database, root);

        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            CatalogQueryRepository repository = new CatalogQueryRepository(catalogDatabase.connection());
            List<CatalogEntry> byName = repository.find("foo.mp4", null, null, null);
            List<CatalogEntry> byNameAndSize = repository.find("foo.mp4", null, Files.size(movie), null);
            assertEquals(1, byName.size());
            assertEquals(1, byNameAndSize.size());
            assertEquals("Foo.MP4", byName.getFirst().filename());
        }
    }

    @Test
    void removesEntriesForFilesThatDisappear() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Path file = Files.writeString(root.resolve("remove-me.txt"), "gone soon");
        Path database = tempDir.resolve("catalogue.db");

        scan(database, root);
        Files.delete(file);
        scan(database, root);

        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            CatalogQueryRepository repository = new CatalogQueryRepository(catalogDatabase.connection());
            assertTrue(repository.find("remove-me.txt", null, null, null).isEmpty());
        }
    }

    @Test
    void skipsDotfilesAndDotDirectories() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Files.writeString(root.resolve("visible.txt"), "yes");
        Files.writeString(root.resolve(".hidden.txt"), "no");
        Path hiddenDirectory = Files.createDirectory(root.resolve(".hidden-dir"));
        Files.writeString(hiddenDirectory.resolve("nested.txt"), "no");
        Path database = tempDir.resolve("catalogue.db");

        scan(database, root);

        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            CatalogQueryRepository repository = new CatalogQueryRepository(catalogDatabase.connection());
            assertEquals(1, repository.find("visible.txt", null, null, null).size());
            assertTrue(repository.find(".hidden.txt", null, null, null).isEmpty());
            assertTrue(repository.find("nested.txt", null, null, null).isEmpty());
        }
    }

    @Test
    void doesNotFollowSymbolicLinksWhenSupported() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Path outside = Files.createDirectory(tempDir.resolve("outside"));
        Files.writeString(outside.resolve("outside.txt"), "no");
        tryCreateSymbolicLink(root.resolve("linked"), outside);
        Path database = tempDir.resolve("catalogue.db");

        scan(database, root);

        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            CatalogQueryRepository repository = new CatalogQueryRepository(catalogDatabase.connection());
            assertTrue(repository.find("outside.txt", null, null, null).isEmpty());
        }
    }

    @Test
    void listsChildrenAndReportsStats() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("media"));
        Path directory = Files.createDirectory(root.resolve("shows"));
        Files.writeString(directory.resolve("episode.txt"), "hello");
        Path database = tempDir.resolve("catalogue.db");

        scan(database, root);

        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            CatalogQueryRepository repository = new CatalogQueryRepository(catalogDatabase.connection());
            List<CatalogEntry> rootChildren = repository.listChildren(root.toString());
            List<CatalogEntry> children = repository.listChildren(root + "/shows");
            CatalogStats stats = repository.stats();
            assertEquals(1, rootChildren.size());
            assertEquals("shows", rootChildren.getFirst().filename());
            assertEquals(1, children.size());
            assertEquals("episode.txt", children.getFirst().filename());
            assertEquals(1, stats.roots());
            assertEquals(2, stats.directories());
            assertEquals(1, stats.files());
            assertFalse(stats.bytes() == 0);
        }
    }

    private void scan(Path database, Path root) throws IOException, SQLException {
        CatalogConfig config = new CatalogConfig(database, Map.of("media", root));
        try (CatalogDatabase catalogDatabase = CatalogDatabase.open(database)) {
            new CatalogScanner(catalogDatabase).scan(config, null);
        }
    }

    private static void tryCreateSymbolicLink(Path link, Path target) throws IOException {
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException ex) {
            // Some Windows environments cannot create symlinks without extra privileges.
        }
    }
}

