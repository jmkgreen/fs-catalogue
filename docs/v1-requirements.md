# Version 1 Requirements

## Purpose

Filesystem Catalogue maintains a cheap, searchable SQLite catalogue of files and directories beneath one or more configured filesystem roots.

The first version is intentionally metadata-only. It must help answer questions about paths, filenames, sizes, extensions, modification times and directory contents without opening or hashing file contents.

## Users

The primary user is a technical home-server user running the application on a TrueNAS Scale host. The user may access the same data over SMB from desktop machines, but the application itself should treat configured roots as ordinary mounted container paths.

Advanced users should be able to query the SQLite database directly.

## Data Scope

The catalogue should record:

* configured roots, optionally with stable human-readable names;
* directories;
* ordinary files;
* path;
* filename;
* size in bytes;
* modification time;
* extension where one can be derived from the filename.

The catalogue should support at least 12 TB of mixed data, including images, videos, text files, archives, folders and PDFs. It should tolerate large volumes of small files, including many files in the 50-500 KB range.

## Scan Behaviour

`catalog scan` should scan all configured roots by default.

It should also support scanning one configured root by name, for example:

```plaintext
catalog scan --root media
```

Scans should:

* recursively enumerate configured roots;
* skip dotfiles and dot-directories by default;
* skip Windows hidden files where this can be detected cheaply;
* not follow symlinks;
* record symlinks only if this is cheap and clearly represented as a non-followed entry;
* remove catalogue entries for files and directories that no longer exist.

A scan must not read file contents. In particular, a scan of a multi-terabyte filesystem must not become a multi-terabyte read operation.

## Search Behaviour

Filename searches should be case-insensitive by default.

The application should make it easy to search by filename and size together, because this is useful for spotting probable duplicates without doing content hashing.

Example questions:

```plaintext
Where is foo.mp4?
Do I have a file called foo.mp4 of size 18372819?
What files are exactly 18372819 bytes?
What matching filenames exist anywhere under this hierarchy?
What files exist under this directory?
```

## Non-Goals

Version 1 should not:

* hash files;
* open media files;
* inspect file contents;
* generate thumbnails;
* detect confirmed duplicates;
* provide a web UI;
* run a web server;
* watch the filesystem continuously;
* modify indexed files.

Probable duplicate discovery based on filename and size is acceptable. Confirmed duplicate detection is not.

## Implementation Direction

Use:

* Java 21;
* picocli;
* JDBC;
* SQLite;
* Flyway or a small explicit migration mechanism;
* JUnit 5.

Spring Boot is not expected to add enough value for version 1.
