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
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

@Command(name = "ls", description = "List entries directly under a catalogued directory.")
public final class LsCommand implements Callable<Integer> {
    @ParentCommand
    private CatalogApp app;

    @Parameters(index = "0", description = "Directory path as mounted in the scanned root.")
    private String path;

    @Option(names = "--json", description = "Write JSON output.")
    private boolean json;

    @Option(names = "--ndjson", description = "Write newline-delimited JSON output.")
    private boolean ndjson;

    @Override
    public Integer call() throws Exception {
        try (CatalogDatabase database = CatalogDatabase.open(Path.of(app.databasePath()))) {
            CatalogQueryRepository repository = new CatalogQueryRepository(database.connection());
            List<CatalogEntry> entries = repository.listChildren(path);
            new OutputWriter(System.out).write(entries, json, ndjson);
        }
        return 0;
    }
}
