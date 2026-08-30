# Development Guardrails

## Required Local Checks

Before committing implementation changes, run:

```plaintext
gradle --warning-mode=fail check
```

This compiles with Java 21, runs tests, runs Checkstyle, and generates a JaCoCo test report.

If Docker is available, also run:

```plaintext
docker build -t fs-catalogue:dev .
```

The Docker build proves the application can be assembled into the same runtime shape expected on TrueNAS Scale.

## Quality Defaults

The build is intentionally strict from the start:

* Java compilation uses `-Xlint:all` and `-Werror`;
* tests use JUnit 5;
* Checkstyle blocks unused imports and star imports;
* JaCoCo reports are generated as part of `check`;
* JaCoCo enforces a minimum 80% instruction coverage threshold.

## Continuous Integration

The GitHub workflow runs on pushes and pull requests targeting `main`.

CI performs:

* Java 21 setup;
* Gradle `check` with `--warning-mode=fail`;
* Docker image build for pull requests and pushes;
* GHCR image publish on successful pushes to `main`;
* GHCR image publish for version tags starting with `v`.

Published images are tagged with `latest` on the default branch, the branch name, a `sha-<commit>` tag, and semantic version tags for releases such as `v0.1.0`.

## Notes

This repository currently expects Gradle to be available locally or provided by CI/Docker. A Gradle wrapper can be added later if we want fully self-contained local builds.

## Docker Build Context

The repository includes a .dockerignore so local build output, Git metadata, SQLite databases and editor state are not sent to Docker during image builds.

## GitHub Actions Versions

The workflow uses Node 24-compatible major versions of the GitHub and Docker actions so CI does not emit Node 20 deprecation warnings. Each job configures Git's default initial branch as main before checkout to avoid Git's master branch-name hint in runner logs.
