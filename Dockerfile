FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon installDist

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/build/install/catalog /app
ENV CATALOG_CONFIG=/config/catalogue.yml
ENV CATALOG_DB=/data/catalogue.db
ENV PATH="/app/bin:${PATH}"
RUN mkdir -p /data /config
ENTRYPOINT ["catalog"]
CMD ["--help"]
