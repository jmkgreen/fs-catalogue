package com.fscatalogue.cli;

import com.fscatalogue.db.CatalogDatabase;
import com.fscatalogue.query.CatalogQueryRepository;
import com.fscatalogue.query.CatalogStats;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "stats", description = "Show catalogue statistics.")
public final class StatsCommand implements Callable<Integer> {
    @ParentCommand
    private CatalogApp app;

    @Option(names = "--json", description = "Write JSON output.")
    private boolean json;

    @Override
    public Integer call() throws Exception {
        try (CatalogDatabase database = CatalogDatabase.open(Path.of(app.databasePath()))) {
            CatalogStats stats = new CatalogQueryRepository(database.connection()).stats();
            if (json) {
                System.out.printf(
                        "{\"roots\":%d,\"directories\":%d,\"files\":%d,\"bytes\":%d}%n",
                        stats.roots(),
                        stats.directories(),
                        stats.files(),
                        stats.bytes());
            } else {
                System.out.printf("Roots: %d%n", stats.roots());
                System.out.printf("Directories: %d%n", stats.directories());
                System.out.printf("Files: %d%n", stats.files());
                System.out.printf("Bytes: %d%n", stats.bytes());
            }
        }
        return 0;
    }
}
