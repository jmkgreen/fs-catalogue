# Development Guardrails

## Required Local Checks

Before committing implementation changes, run:

```plaintext
gradle check
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
* Gradle `check`;
* Docker image build;
* GHCR image publish on successful pushes to `main`.

## Notes

This repository currently expects Gradle to be available locally or provided by CI/Docker. A Gradle wrapper can be added later if we want fully self-contained local builds.
