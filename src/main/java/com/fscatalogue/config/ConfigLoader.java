package com.fscatalogue.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public final class ConfigLoader {
    private ConfigLoader() {
    }

    public static CatalogConfig load(Path configPath, Path defaultDatabasePath) throws IOException {
        if (!Files.exists(configPath)) {
            throw new IOException("Config file does not exist: " + configPath);
        }

        Object loaded;
        try (InputStream input = Files.newInputStream(configPath)) {
            loaded = new Yaml().load(input);
        }
        if (!(loaded instanceof Map<?, ?> rootMap)) {
            throw new IOException("Config file must contain a YAML mapping.");
        }

        Path databasePath = defaultDatabasePath;
        Object databaseValue = rootMap.get("database");
        if (databaseValue instanceof String databaseString && !databaseString.isBlank()) {
            databasePath = Path.of(databaseString);
        }

        Object rootsValue = rootMap.get("roots");
        if (!(rootsValue instanceof Map<?, ?> rootsMap) || rootsMap.isEmpty()) {
            throw new IOException("Config file must define at least one root.");
        }

        Map<String, Path> roots = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : rootsMap.entrySet()) {
            if (!(entry.getKey() instanceof String name) || name.isBlank()) {
                throw new IOException("Root names must be non-empty strings.");
            }
            if (!(entry.getValue() instanceof String path) || path.isBlank()) {
                throw new IOException("Root path for '" + name + "' must be a non-empty string.");
            }
            roots.put(name, Path.of(path));
        }

        return new CatalogConfig(databasePath, roots);
    }
}
