package com.fscatalogue.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

final class OutputWriterTest {
    private final CatalogEntry entry = new CatalogEntry(
            "media",
            "/mnt/media",
            "clips/Foo \"bar\".txt",
            "Foo \"bar\".txt",
            "file",
            5L,
            "2026-08-30T10:15:30Z",
            "txt");

    @Test
    void writesHumanReadablePaths() {
        assertEquals("/mnt/media/clips/Foo \"bar\".txt%n".formatted(), write(false, false));
    }

    @Test
    void writesJsonArray() {
        String output = write(true, false);

        assertEquals("[", output.substring(0, 1));
        assertEquals(true, output.endsWith("]" + System.lineSeparator()));
    }

    @Test
    void writesNdjson() {
        String output = write(false, true);

        assertEquals(1, output.lines().count());
    }

    @Test
    void rejectsJsonAndNdjsonTogether() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputWriter writer = new OutputWriter(new PrintStream(bytes, true, StandardCharsets.UTF_8));

        assertThrows(IllegalArgumentException.class, () -> writer.write(List.of(entry), true, true));
    }

    private String write(boolean json, boolean ndjson) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        OutputWriter writer = new OutputWriter(new PrintStream(bytes, true, StandardCharsets.UTF_8));
        writer.write(List.of(entry), json, ndjson);
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
