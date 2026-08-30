package com.fscatalogue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fscatalogue.config.CatalogConfig;
import com.fscatalogue.config.ConfigLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class ConfigLoaderTest {
    @TempDir
    private Path tempDir;

    @Test
    void loadsYamlConfig() throws Exception {
        Path config = tempDir.resolve("catalogue.yml");
        Files.writeString(config, """
                database: /data/catalogue.db
                roots:
                  media: /mnt/media
                  backups: /mnt/backups
                """);

        CatalogConfig loaded = ConfigLoader.load(config, Path.of("fallback.db"));

        assertEquals(Path.of("/data/catalogue.db"), loaded.databasePath());
        assertEquals(Path.of("/mnt/media"), loaded.roots().get("media"));
        assertEquals(Path.of("/mnt/backups"), loaded.roots().get("backups"));
    }
}
