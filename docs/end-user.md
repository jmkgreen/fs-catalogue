# End-User Guide

## What This Application Expects

Filesystem Catalogue expects to run as a command-line application with access to:

* one writable data directory for the SQLite catalogue;
* one readable configuration file;
* one or more readable filesystem roots to scan.

When running with Docker, those are usually mounted as:

```plaintext
/data
/config/catalogue.yml
/mnt/media
/mnt/backups
```

The application does not mount SMB shares itself. If files are on a TrueNAS Scale host, expose them to the container using Docker volumes. It does not matter that you may access the same files from another computer over SMB.

## What It Creates

The application creates and updates the SQLite database configured by `database` in `catalogue.yml`, or by `CATALOG_DB` / `--db` if supplied.

For the default Docker setup, expect these files under `./data` on the host:

```plaintext
catalogue.db
catalogue.db-shm
catalogue.db-wal
```

The `-shm` and `-wal` files are normal SQLite WAL-mode side files. They help readers query while scans are writing.

The application should not create, edit or delete anything inside the scanned roots.

## Configuration File

Create `config/catalogue.yml` before scanning.

Example:

```yaml
database: /data/catalogue.db
roots:
  media: /mnt/media
  backups: /mnt/backups
```

`database` is the SQLite database path as seen from inside the container.

`roots` maps a stable root name to a mounted directory path as seen from inside the container. Root names are used by commands such as:

```plaintext
catalog scan --root media
catalog find --root media --name "foo.mp4"
```

Choose root names that are short, stable and meaningful. If a root name changes, the application treats it as a different configured root.

## Container Startup Behaviour

Starting the Docker container does not start a scan.

With the example Docker Compose file, the container starts and then waits. It does not create scheduled jobs, run periodically, watch the filesystem or expose a web server.

This is intentional. The container provides the `catalog` CLI in a stable environment. The end-user, or a host scheduler, runs commands when needed:

```plaintext
docker exec fs-catalogue catalog scan
docker exec fs-catalogue catalog find --name "foo.mp4"
docker exec fs-catalogue catalog stats
```

For regular updates, schedule `docker exec fs-catalogue catalog scan` from TrueNAS or host cron.

## First Run

From a Docker Compose deployment, start the idle container:

```plaintext
docker compose up -d
```

Then run the first scan manually:

```plaintext
docker exec fs-catalogue catalog scan
```

Query the catalogue:

```plaintext
docker exec fs-catalogue catalog find --name "foo.mp4"
docker exec fs-catalogue catalog find --name "foo.mp4" --size 18372819
docker exec fs-catalogue catalog find --name-like "*holiday*" --json
docker exec fs-catalogue catalog ls /mnt/media/some-folder
docker exec fs-catalogue catalog stats
```

## Scan Rules

A scan records directory and ordinary-file metadata only:

* path;
* filename;
* file size;
* modified time;
* extension.

A scan does not read file contents, hash files, inspect media, generate thumbnails or detect confirmed duplicates.

By default, scans:

* scan every configured root;
* skip dotfiles and dot-directories;
* skip hidden files where that can be detected cheaply;
* do not follow symlinks;
* remove catalogue entries for files and directories that no longer exist.

## Scheduling

The application does not include an internal scheduler. The simplest scheduled setup is to keep the container running and call the CLI from the host scheduler.

Example nightly command:

```plaintext
docker exec fs-catalogue catalog scan
```

Queries can still run while the container is up. SQLite WAL mode is enabled to make read access during scans more practical.

## Direct SQLite Access

Advanced users can read the SQLite database directly. Treat the scanner as the only writer.

If another application wants to use the catalogue database, mount `./data` read-only and open `catalogue.db` read-only where possible.

## Safety Expectations

The scanned roots should usually be mounted read-only in Docker.

That gives an extra guardrail: even if the application has a bug, the container should not have permission to modify indexed files.
