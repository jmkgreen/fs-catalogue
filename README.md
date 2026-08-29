# A Filesystem Catalogue

A database of your filesystem.

## Goal

Maintain a cheap, searchable SQLite catalogue of files in one or more configured filesystem roots.

It answers questions such as:

```plaintext
Where is foo.mp4?
Do I have a file called foo.mp4 of size 18372819?
What files are exactly 18372819 bytes?
What matching filenames exist anywhere under this hierarchy?
What files exist under this directory?
```

It does not initially understand photos, videos or duplicate content.

## Responsibilities

It should:

* recursively enumerate configured roots;
* record directories and ordinary files;
* maintain path, filename, size, mtime and extension;
* efficiently update an existing catalogue;
* remove catalogue entries for files that have disappeared;
* expose queries through a CLI;
* support machine-readable JSON/NDJSON output;
* permit direct SQLite querying by advanced users;
* provide schema migration/versioning.

A sensible initial CLI might be:

```plaintext
catalog scan
catalog find --name "foo.mp4"
catalog find --name "foo.mp4" --size 17263722
catalog find --name-like "*foo*"
catalog find --size 17263722
catalog ls /mnt/media/foo
catalog stats
```

with:

`--json`

on every query command.

## Explicit non-goals

Version 1 should not:

* hash files;
* open media files;
* generate thumbnails;
* detect duplicates;
* provide a web UI;
* run a web server;
* watch the filesystem continuously;
* modify indexed files.

A scan of 12 terabytes must not accidentally turn into 12 terabytes of reads.

## Suggested implementation

This is a good candidate for Java 21, probably as a plain command-line application rather than Spring Boot.

I'd be inclined toward:

Java 21;
picocli;
JDBC;
SQLite;
Flyway or a very small explicit schema migration mechanism;
JUnit 5.

Spring adds little here.

## Current direction

Version 1 should be a Java 21 command-line application packaged for Docker.

The application is expected to run on the TrueNAS host with filesystem roots mounted into the container using ordinary Docker volumes. It should not need to know that the end-user may access the same files over SMB from another machine.

The SQLite catalogue should be stored under `/data`, for example `/data/catalogue.db`, so that other tools can mount the same database read-only later if they want to provide a separate query interface.

## Documentation

Focused requirements live under `docs/`:

* [Version 1 requirements](docs/v1-requirements.md)
* [CLI requirements](docs/cli.md)
* [Storage and schema notes](docs/storage-schema.md)
* [Docker and operations](docs/docker.md)
