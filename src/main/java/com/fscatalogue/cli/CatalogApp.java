package com.fscatalogue.cli;

import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "catalog",
        mixinStandardHelpOptions = true,
        version = "fs-catalogue 0.1.0",
        description = "Searchable SQLite catalogue of filesystem metadata.",
        subcommands = {
                ScanCommand.class,
                FindCommand.class,
                LsCommand.class,
                StatsCommand.class
        })
public final class CatalogApp implements Callable<Integer> {
    @Option(names = "--db", description = "SQLite database path. Defaults to ${DEFAULT-VALUE}.")
    private String databasePath = defaultFromEnv("CATALOG_DB", "/data/catalogue.db");

    @Option(names = "--config", description = "YAML config path. Defaults to ${DEFAULT-VALUE}.")
    private String configPath = defaultFromEnv("CATALOG_CONFIG", "/config/catalogue.yml");

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CatalogApp()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    String databasePath() {
        return databasePath;
    }

    String configPath() {
        return configPath;
    }

    private static String defaultFromEnv(String name, String fallback) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
