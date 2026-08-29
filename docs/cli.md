# CLI Requirements

## Command Shape

The CLI executable should be called `catalog` in examples.

Initial commands:

```plaintext
catalog scan
catalog scan --root media
catalog find --name "foo.mp4"
catalog find --name "foo.mp4" --size 17263722
catalog find --name-like "*foo*"
catalog find --size 17263722
catalog ls /mnt/media/foo
catalog stats
```

## Global Options

The CLI should allow the database location to be configured.

Likely options:

```plaintext
--db /data/catalogue.db
--config /config/catalogue.yml
```

The default database path in Docker should be:

```plaintext
/data/catalogue.db
```

## Configuration

The application should support multiple named roots.

Example configuration:

```yaml
database: /data/catalogue.db
roots:
  media: /mnt/media
  backups: /mnt/backups
```

Root names should be stable identifiers used in scan commands and stored query results.

## Query Output

Human-readable output should be the default.

Every query command should support JSON output:

```plaintext
catalog find --name "foo.mp4" --json
catalog ls /mnt/media/foo --json
catalog stats --json
```

NDJSON can be added for commands that may produce large result sets:

```plaintext
catalog find --name-like "*.jpg" --ndjson
```

JSON schemas can evolve during early development, but field names should be kept boring and predictable once the first usable version exists.

## Search Semantics

Filename matching should be case-insensitive by default.

`--name` should match a complete filename.

`--name-like` should support simple wildcard matching suitable for shell users. The implementation may translate this into a SQLite `LIKE` query with escaping rules documented later.

`--size` should match exact byte size.

## Exit Codes

Suggested exit codes:

* `0`: command succeeded;
* `1`: command failed because of invalid input, missing config, inaccessible database or scan/query error;
* `2`: command syntax error.

Search commands should return `0` even when no files match. An empty result is not an error.
