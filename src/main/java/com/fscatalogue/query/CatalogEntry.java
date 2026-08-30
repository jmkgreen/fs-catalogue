package com.fscatalogue.query;

public record CatalogEntry(
        String root,
        String rootPath,
        String relativePath,
        String filename,
        String type,
        Long sizeBytes,
        String modifiedTime,
        String extension) {
    public String mountedPath() {
        if (relativePath == null || relativePath.isEmpty()) {
            return rootPath;
        }
        return rootPath + "/" + relativePath;
    }
}
