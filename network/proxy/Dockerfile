# syntax=docker/dockerfile:1
# Velocity proxy for the lobby network in network/. Build context: the repository root (see network/compose.yaml).
# Velocity 4.x needs Java 25.
ARG JAVA_VERSION=25

FROM eclipse-temurin:${JAVA_VERSION}-jdk AS build
ARG JAVA_VERSION
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl jq ca-certificates \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /build

COPY tools/ tools/
RUN javac --release 17 -d classes tools/*.java \
 && jar --create --file mctool.jar --main-class McTool -C classes .

ARG VELOCITY_VERSION=latest
# Changing CACHE_BUST forces "latest" to be re-resolved instead of reusing the cached layer.
ARG CACHE_BUST=0
COPY network/proxy/fetch-velocity.sh .
RUN bash fetch-velocity.sh "$VELOCITY_VERSION" "$JAVA_VERSION" /build/out/velocity.jar \
 && cd /build/out && jar xf velocity.jar default-velocity.toml && test -s default-velocity.toml

FROM eclipse-temurin:${JAVA_VERSION}-jre
RUN apt-get update \
 && apt-get install -y --no-install-recommends tini \
 && rm -rf /var/lib/apt/lists/*

COPY --from=build /build/out/ /opt/velocity/
COPY --from=build /build/mctool.jar /opt/velocity/mctool.jar
COPY --chmod=755 scripts/entrypoint.sh          /usr/local/bin/entrypoint
COPY --chmod=755 network/proxy/start-proxy.sh   /usr/local/bin/start-proxy
COPY --chmod=755 network/proxy/proxy-plugins    /usr/local/bin/plugins

LABEL org.opencontainers.image.title="Minecraft Network Proxy (Velocity)" \
      org.opencontainers.image.source="https://github.com/agrawalchaitany/minecraft-server" \
      org.opencontainers.image.description="Self-built Velocity proxy for the lobby network" \
      org.opencontainers.image.licenses="MIT"

ENV HOME=/data
WORKDIR /data
VOLUME ["/data"]
EXPOSE 25565

STOPSIGNAL SIGTERM
HEALTHCHECK --interval=30s --timeout=10s --start-period=2m --retries=3 \
  CMD java -XX:TieredStopAtLevel=1 -Xmx32m -jar /opt/velocity/mctool.jar ping 127.0.0.1 25565 >/dev/null || exit 1
ENTRYPOINT ["tini", "--", "entrypoint"]
CMD ["start-proxy"]
