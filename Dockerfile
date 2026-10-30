# syntax=docker/dockerfile:1
# Java for the server; `make` picks it automatically from MC_VERSION (17, 21 or 25).
ARG JAVA_VERSION=25
# Java for building the tooling; the tools themselves target Java 17 so they run on any server Java.
ARG BUILD_JDK=25

FROM eclipse-temurin:${BUILD_JDK}-jdk AS build
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl jq ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /build

COPY tools/ tools/
RUN javac --release 17 -d classes tools/*.java \
 && jar --create --file mctool.jar --main-class McTool -C classes .

ARG MC_VERSION=latest
ARG PAPER_BUILD=latest
ARG JAVA_VERSION
# Changing CACHE_BUST forces "latest" to be re-resolved instead of reusing the cached layer.
ARG CACHE_BUST=0
COPY scripts/fetch-paper.sh .
RUN JAVA_VERSION="$JAVA_VERSION" bash fetch-paper.sh "$MC_VERSION" "$PAPER_BUILD" /build/out/paper.jar

# Compile bundled plugins against the exact Paper API shipped inside the downloaded server.
COPY plugins/username-rules/ plugins/username-rules/
# Paperclip can exit 0 after a failed download, so verify the patched server actually exists and retry.
RUN mkdir patched && cd patched \
 && for i in 1 2 3; do \
      java -Dpaperclip.patchonly=true -jar /build/out/paper.jar >/dev/null 2>&1; \
      ls versions/*/*.jar cache/patched_*.jar 2>/dev/null | grep -q . && break; \
      echo "Patching Paper failed (attempt $i), retrying"; rm -rf ./*; sleep 5; \
    done \
 && ls versions/*/*.jar cache/patched_*.jar 2>/dev/null | grep -q . \
 && cp="$(find "$PWD" -name '*.jar' | tr '\n' ':')" \
 && cd /build/plugins/username-rules \
 && javac --release "$JAVA_VERSION" -cp "$cp" -d classes src/*.java \
 && cp plugin.yml classes/ \
 && mkdir -p /build/out/plugins \
 && jar --create --file /build/out/plugins/UsernameRules.jar -C classes .

FROM eclipse-temurin:${JAVA_VERSION}-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends tini \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/out/ /opt/minecraft/
COPY --from=build /build/mctool.jar /opt/minecraft/mctool.jar
COPY --chmod=755 scripts/entrypoint.sh     /usr/local/bin/entrypoint
COPY --chmod=755 scripts/start-server.sh   /usr/local/bin/start-server
COPY --chmod=755 scripts/backup-now.sh     /usr/local/bin/backup-now
COPY --chmod=755 scripts/backup-loop.sh    /usr/local/bin/backup-loop
COPY --chmod=755 scripts/restore-backup.sh /usr/local/bin/restore-backup
COPY --chmod=755 scripts/rcon-cli          /usr/local/bin/rcon-cli
COPY --chmod=755 scripts/mc-health         /usr/local/bin/mc-health
COPY --chmod=755 scripts/plugins           /usr/local/bin/plugins

ENV HOME=/data
WORKDIR /data
VOLUME ["/data"]
EXPOSE 25565

STOPSIGNAL SIGTERM
HEALTHCHECK --interval=30s --timeout=10s --start-period=5m --retries=3 CMD mc-health
ENTRYPOINT ["tini", "--", "entrypoint"]
CMD ["server"]
