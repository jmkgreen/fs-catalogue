package com.fscatalogue.cli;

import com.fscatalogue.db.CatalogDatabase;
import com.fscatalogue.query.CatalogEntry;
import com.fscatalogue.query.CatalogQueryRepository;
import com.fscatalogue.query.OutputWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "find", description = "Find catalogue entries by filename and size.")
public final class FindCommand implements Callable<Integer> {
    @ParentCommand
    private CatalogApp app;

    @Option(names = "--name", description = "Exact filename, matched case-insensitively.")
    private String name;

    @Option(names = "--name-like", description = "Wildcard filename pattern, matched case-insensitively.")
    private String nameLike;

    @Option(names = "--size", description = "Exact file size in bytes.")
    private Long size;

    @Option(names = "--root", description = "Restrict results to one configured root name.")
    private String root;

    @Option(names = "--json", description = "Write JSON output.")
    private boolean json;

    @Option(names = "--ndjson", description = "Write newline-delimited JSON output.")
    private boolean ndjson;

    @Override
    public Integer call() throws Exception {
        if (name == null && nameLike == null && size == null) {
            throw new IllegalArgumentException("At least one of --name, --name-like or --size is required.");
        }
        if (name != null && nameLike != null) {
            throw new IllegalArgumentException("Use either --name or --name-like, not both.");
        }

        try (CatalogDatabase database = CatalogDatabase.open(Path.of(app.databasePath()))) {
            CatalogQueryRepository repository = new CatalogQueryRepository(database.connection());
            List<CatalogEntry> entries = repository.find(name, nameLike, size, root);
            new OutputWriter(System.out).write(entries, json, ndjson);
        }
        return 0;
    }
}
