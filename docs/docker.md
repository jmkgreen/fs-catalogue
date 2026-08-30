# Docker and Operations

## Runtime Model

The application is packaged as a Docker image intended to run on the TrueNAS Scale host.

Filesystem roots are mounted into the container using ordinary Docker volumes. The application sees those mounts as local paths. It does not know or care whether the same files are also shared to end-user devices over SMB.

The example container is deliberately idle after startup. It remains running so scans and queries can be invoked later with `docker exec`.

Starting the container does not scan files, create scheduled jobs or start a background worker. A scan happens only when something runs `catalog scan`.

## Expected Host Layout

A simple deployment can use this folder layout next to `docker-compose.yml`:

```plaintext
fs-catalogue/
  config/
    catalogue.yml
  data/
    catalogue.db
    catalogue.db-shm
    catalogue.db-wal
  docker-compose.yml
```

Create `config/catalogue.yml` before the first scan. The `data` directory can start empty; the application creates the SQLite database there.

The scanned files themselves do not need to live under this deployment folder. On TrueNAS they will normally already exist somewhere such as `/mnt/tank/media` and be mounted into the container read-only.

## Configuration

The default Docker setup expects:

```plaintext
/config/catalogue.yml
/data/catalogue.db
```

Example `config/catalogue.yml` on the host:

```yaml
database: /data/catalogue.db
roots:
  media: /mnt/media
  backups: /mnt/backups
```

The paths in `catalogue.yml` are container paths, not Windows desktop paths and not SMB UNC paths.

Each entry under `roots` is:

* a stable root name, such as `media`;
* the path where that dataset is mounted inside the container, such as `/mnt/media`.

Root names are used by commands such as:

```plaintext
catalog scan --root media
catalog find --root media --name "foo.mp4"
```

## Volumes

Suggested container paths:

```plaintext
/data
/config
/mnt/media
/mnt/backups
```

`/data` is writable and holds the SQLite catalogue.

`/config` can be read-only and holds `catalogue.yml`.

Catalogue roots such as `/mnt/media` and `/mnt/backups` should usually be mounted read-only. The application is designed not to modify indexed files, and read-only mounts provide an extra safety guardrail.

## Example docker-compose.yml

```yaml
services:
  catalog:
    build: .
    image: fs-catalogue:latest
    container_name: fs-catalogue
    restart: unless-stopped
    volumes:
      - ./data:/data
      - ./config:/config:ro
      - /mnt/tank/media:/mnt/media:ro
      - /mnt/tank/backups:/mnt/backups:ro
    environment:
      CATALOG_CONFIG: /config/catalogue.yml
      CATALOG_DB: /data/catalogue.db
    entrypoint: ["sleep", "infinity"]
```

Adjust the left-hand side of the media/backups mounts to match the real TrueNAS dataset paths.

The right-hand side must match the paths used in `config/catalogue.yml`.

## Container Startup Behaviour

With the example Compose file, `docker compose up -d` starts a long-running idle container.

Nothing else happens automatically:

* no filesystem scan starts;
* no periodic job is registered;
* no cron process starts inside the container;
* no web server starts;
* no filesystem watcher starts.

The end-user is expected to run commands against the container, for example:

```plaintext
docker exec fs-catalogue catalog scan
docker exec fs-catalogue catalog stats
docker exec fs-catalogue catalog find --name "foo.mp4"
```

A host-level scheduler, such as the TrueNAS scheduler or cron, can run the same `docker exec fs-catalogue catalog scan` command overnight.

## First Run

Start the long-running idle container:

```plaintext
docker compose up -d
```

Run a scan of every configured root:

```plaintext
docker exec fs-catalogue catalog scan
```

Run a scan of one configured root:

```plaintext
docker exec fs-catalogue catalog scan --root media
```

Run queries:

```plaintext
docker exec fs-catalogue catalog find --name "foo.mp4"
docker exec fs-catalogue catalog find --name "foo.mp4" --size 18372819
docker exec fs-catalogue catalog find --name-like "*foo*" --json
docker exec fs-catalogue catalog ls /mnt/media/foo
docker exec fs-catalogue catalog stats
```

## Scheduling

Version 1 expects scheduling to happen outside the application, around the CLI. It does not include built-in periodic jobs, an internal cron service or a filesystem watcher.

Recommended approach:

```plaintext
docker exec fs-catalogue catalog scan
```

Run that command from the TrueNAS scheduler or host cron overnight.

Other possible approaches:

* host cron runs `docker exec fs-catalogue catalog scan`;
* TrueNAS scheduling invokes the container command.

## Concurrent Access

The SQLite database may be mounted read-only by other tools later, including a separate query interface.

The scanner should be treated as the only writer. Query commands should tolerate the database being updated by a scan.

SQLite WAL mode is enabled by the application, so it is normal to see these files:

```plaintext
catalogue.db
catalogue.db-shm
catalogue.db-wal
```

Any external reader should account for WAL mode and should open the database read-only if it is not responsible for scanning.

## What Docker Does Not Do

The Docker setup does not:

* mount SMB shares;
* create TrueNAS datasets;
* grant permissions to files the container user cannot read;
* expose a web UI;
* run a web server;
* watch files continuously;
* run periodic jobs by itself.
