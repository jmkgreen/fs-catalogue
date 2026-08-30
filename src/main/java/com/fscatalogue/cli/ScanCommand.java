package com.fscatalogue.cli;

import com.fscatalogue.config.CatalogConfig;
import com.fscatalogue.config.ConfigLoader;
import com.fscatalogue.db.CatalogDatabase;
import com.fscatalogue.scan.CatalogScanner;
import com.fscatalogue.scan.ScanSummary;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "scan", description = "Scan configured filesystem roots.")
public final class ScanCommand implements Callable<Integer> {
    @ParentCommand
    private CatalogApp app;

    @Option(names = "--root", description = "Scan one configured root by name.")
    private String rootName;

    @Override
    public Integer call() throws Exception {
        CatalogConfig config = ConfigLoader.load(Path.of(app.configPath()), Path.of(app.databasePath()));
        try (CatalogDatabase database = CatalogDatabase.open(config.databasePath())) {
            ScanSummary summary = new CatalogScanner(database).scan(config, rootName);
            System.out.printf(
                    "Scanned %d root(s), %d directories, %d file(s).%n",
                    summary.rootsScanned(),
                    summary.directoriesSeen(),
                    summary.filesSeen());
        }
        return 0;
    }
}

