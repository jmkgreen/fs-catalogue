package com.fscatalogue.query;

import java.io.PrintStream;
import java.util.List;

public final class OutputWriter {
    private final PrintStream output;

    public OutputWriter(PrintStream output) {
        this.output = output;
    }

    public void write(List<CatalogEntry> entries, boolean json, boolean ndjson) {
        if (json && ndjson) {
            throw new IllegalArgumentException("Use either --json or --ndjson, not both.");
        }
        if (ndjson) {
            for (CatalogEntry entry : entries) {
                output.println(toJson(entry));
            }
            return;
        }
        if (json) {
            output.print("[");
            for (int i = 0; i < entries.size(); i++) {
                if (i > 0) {
                    output.print(",");
                }
                output.print(toJson(entries.get(i)));
            }
            output.println("]");
            return;
        }
        for (CatalogEntry entry : entries) {
            output.println(entry.mountedPath());
        }
    }

    private static String toJson(CatalogEntry entry) {
        return "{"
                + property("root", entry.root()) + ","
                + property("rootPath", entry.rootPath()) + ","
                + property("relativePath", entry.relativePath()) + ","
                + property("path", entry.mountedPath()) + ","
                + property("filename", entry.filename()) + ","
                + property("type", entry.type()) + ","
                + nullableNumberProperty("sizeBytes", entry.sizeBytes()) + ","
                + property("modifiedTime", entry.modifiedTime()) + ","
                + property("extension", entry.extension())
                + "}";
    }

    private static String property(String name, String value) {
        return quote(name) + ":" + quote(value);
    }

    private static String nullableNumberProperty(String name, Long value) {
        if (value == null) {
            return quote(name) + ":null";
        }
        return quote(name) + ":" + value;
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> escaped.append("\\\\");
                case '"' -> escaped.append("\\\"");
                case '\n' -> escaped.append("\\n");
                case '\r' -> escaped.append("\\r");
                case '\t' -> escaped.append("\\t");
                default -> escaped.append(character);
            }
        }
        return escaped.append('"').toString();
    }
}
