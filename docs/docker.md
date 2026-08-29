# Docker and Operations

## Runtime Model

The application should be packaged as a Docker image that runs on the TrueNAS Scale host.

Filesystem roots should be mounted into the container using Docker volumes. The application should treat them as ordinary local paths and should not need SMB-specific logic.

The container can remain running so scheduled scans can happen inside it and the user can execute CLI queries with `docker exec`.

## Data Volumes

Suggested container paths:

```plaintext
/data
/config
/mnt/media
/mnt/backups
```

`/data` should hold the SQLite database.

`/config` should hold configuration such as named roots.

Mounted catalogue roots should usually be read-only.

## Example docker-compose.yml

```yaml
services:
  catalog:
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
    command: ["sleep", "infinity"]
```

This keeps the container available for manual commands:

```plaintext
docker exec fs-catalogue catalog scan
docker exec fs-catalogue catalog find --name "foo.mp4"
docker exec fs-catalogue catalog stats
```

## Scheduling

Version 1 can support overnight scans through cron-style scheduling around the CLI.

Possible approaches:

* host cron runs `docker exec fs-catalogue catalog scan`;
* a small cron process runs inside the container;
* TrueNAS scheduling invokes the container command.

The first implementation should not require a long-running filesystem watcher.

## Concurrent Access

The SQLite database may later be mounted read-only by other tools.

The scanner should be the only writer. Query commands should tolerate the database being updated by a scan, using SQLite pragmas or transaction patterns that keep read behaviour predictable.

WAL mode is worth considering so readers can query while scans are running.
