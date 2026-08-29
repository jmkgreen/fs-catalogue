# Storage and Schema Notes

## Database

Use SQLite as the catalogue database.

The default Docker path should be:

```plaintext
/data/catalogue.db
```

The database should be readable by other tools in the future, including a possible separate query interface. The application should avoid assumptions that make direct SQLite reads difficult.

## Identity

Version 1 should identify entries by root plus path.

Do not depend on inode or platform-specific file identity for correctness. The application will often scan container-mounted storage from a NAS, where inode-like identity may be unavailable, unstable or misleading.

## Suggested Tables

The first schema will probably need:

* `schema_version` or an explicit migration table;
* `roots`;
* `entries`;
* `scan_runs`, if useful for diagnostics and stats.

`roots` should store:

* root id;
* root name;
* mounted path;
* created timestamp;
* updated timestamp.

`entries` should store:

* root id;
* relative path;
* parent relative path;
* filename;
* lowercase filename or normalized search filename;
* entry type, at least `directory` and `file`;
* size in bytes for ordinary files;
* modification time;
* extension;
* scan timestamp or scan run id.

## Deletions

If a file or directory disappears during a scan, its catalogue entry should be removed.

Version 1 does not need historical records for deleted files.

## Indexing

Indexes should support:

* exact filename lookup, case-insensitive;
* filename plus size lookup;
* exact size lookup;
* listing children of a directory;
* filtering by root;
* basic stats.

Likely useful indexes:

* `(root_id, relative_path)`;
* `(root_id, parent_relative_path)`;
* `(filename_normalized)`;
* `(filename_normalized, size_bytes)`;
* `(size_bytes)`;
* `(extension)`.

## Migrations

Use Flyway or a deliberately small migration runner.

Migrations should be explicit and checked into source control. The application should fail clearly if the database schema is newer than the application understands.
