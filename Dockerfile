# ============================================================
#  TCKR — multi-stage image
#
#  Stage 1 builds the jar, stage 2 ships only a JRE and the jar. The build
#  toolchain (Maven, the JDK, the local repository) never reaches the final
#  image, which keeps it small and removes a large amount of attack surface
#  that a running API has no use for.
# ============================================================

# ------------------------------------------------------------
#  Stage 1 — build
# ------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Dependencies resolve in their own layer, keyed only on the pom. Docker reuses
# that layer for every build where the pom has not changed, so editing a Java
# file does not re-download the dependency tree.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src

# Tests are deliberately skipped here. SchemaMigrationTest needs a Docker
# daemon via Testcontainers, which is not available inside a build, so running
# the suite here would either skip the one test that matters or fail outright.
# The suite belongs in CI and on a developer machine, where Docker exists.
RUN mvn -B -q clean package -DskipTests

# Unpack the layered jar so each layer can be copied separately below.
RUN java -Djarmode=layertools -jar target/tckr.jar extract --destination /build/layers

# ------------------------------------------------------------
#  Stage 2 — runtime
# ------------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime

# curl is here for the healthcheck below. The base image ships no HTTP client,
# and the shell-only alternatives (/dev/tcp) need bash and parse the response
# by hand, which fails in ways that look like an unhealthy app rather than a
# broken check.
RUN apt-get update \
 && apt-get install --no-install-recommends -y curl \
 && rm -rf /var/lib/apt/lists/*

# Runs unprivileged. A container process that does not need root should not
# have it: it limits what a remote-code-execution bug in the API can reach.
RUN groupadd --system --gid 1001 tckr \
 && useradd --system --uid 1001 --gid tckr --home /app tckr

WORKDIR /app

# Copied in order of how often each changes, so a code edit invalidates only
# the last and smallest layer.
COPY --from=build --chown=tckr:tckr /build/layers/dependencies/ ./
COPY --from=build --chown=tckr:tckr /build/layers/spring-boot-loader/ ./
COPY --from=build --chown=tckr:tckr /build/layers/snapshot-dependencies/ ./
COPY --from=build --chown=tckr:tckr /build/layers/application/ ./

# Logs are configured to logs/ in application.properties, and the app runs as a
# non-root user, so the directory has to exist and be writable up front.
RUN mkdir -p /app/logs && chown tckr:tckr /app/logs

USER tckr

EXPOSE 8080

# Defined in the image rather than only in Compose, so a plain `docker run`
# reports health too. /actuator/health is the only actuator path exposed and
# show-details=never keeps the body to {"status":"UP"}.
#
# start-period is generous because the first boot does real work: Flyway
# applies every migration and Hibernate then validates each entity mapping
# against the result. Treating that as a failed healthcheck would restart the
# container mid-migration.
HEALTHCHECK --interval=10s --timeout=5s --start-period=90s --retries=12 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health | grep -q '"status":"UP"'

# MaxRAMPercentage rather than a fixed -Xmx: the JVM then sizes the heap from
# whatever the container is actually given, so changing the memory limit does
# not require rebuilding the image. 75% leaves headroom for metaspace, thread
# stacks and direct buffers, which live outside the heap.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseContainerSupport"

# No credential, secret or profile is baked in. Image layers are as durable and
# as copyable as git history, which is exactly how the Schwab credentials in
# this repository's history became a standing problem. Everything sensitive
# arrives as an environment variable at run time.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.JarLauncher"]
