package com.fscatalogue.config;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CatalogConfig(Path databasePath, Map<String, Path> roots) {
    public CatalogConfig {
        roots = Collections.unmodifiableMap(new LinkedHashMap<>(roots));
    }
}
