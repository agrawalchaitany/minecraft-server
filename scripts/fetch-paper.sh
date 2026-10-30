#!/usr/bin/env bash
# Usage: fetch-paper.sh <mc-version|latest> <build|latest> <output.jar>
set -euo pipefail

version="${1:-latest}"
build="${2:-latest}"
out="${3:?output path required}"

API="https://fill.papermc.io/v3/projects/paper"
UA="self-hosted-paper-docker/1.0"

api() { curl -fsSL --retry 5 --retry-delay 3 -A "$UA" "${API}$1"; }

stable_build() { api "/versions/$1/builds" | jq -r '[.[] | select(.channel == "STABLE") | .id] | max // empty'; }

if [ "$version" = "latest" ] && [ "$build" != "latest" ]; then
  echo "PAPER_BUILD=$build only makes sense with a specific MC_VERSION (build numbers differ per version)" >&2
  exit 1
fi

if [ "$version" = "latest" ]; then
  for v in $(api "" | jq -r '.versions[][]' | grep -E '^[0-9]+(\.[0-9]+)+$' | sort -rV); do
    if [ -n "$(stable_build "$v")" ]; then version="$v"; break; fi
  done
  [ "$version" != "latest" ] || { echo "No stable Paper version found" >&2; exit 1; }
fi

if [ "$build" = "latest" ]; then
  build="$(stable_build "$version")"
  [ -n "$build" ] || { echo "No stable build for Paper $version" >&2; exit 1; }
fi

need_java="$(api "/versions/$version" | jq -r '.version.java.version.minimum // 0')"
have_java="${JAVA_VERSION:?JAVA_VERSION (Java for the server) must be set}"
if [ "$have_java" -lt "$need_java" ]; then
  echo "Minecraft $version needs Java $need_java but the image is being built with Java $have_java." >&2
  echo "Build with 'make up' (picks Java automatically) or set JAVA_VERSION=$need_java in .env." >&2
  exit 1
fi

meta="$(api "/versions/$version/builds/$build")"
url="$(jq -r '.downloads["server:default"].url' <<<"$meta")"
sha="$(jq -r '.downloads["server:default"].checksums.sha256' <<<"$meta")"

mkdir -p "$(dirname "$out")"
echo "Downloading Paper $version build $build"
curl -fsSL --retry 5 --retry-delay 3 -A "$UA" -o "$out" "$url"
echo "$sha  $out" | sha256sum -c -

printf 'PAPER_VERSION=%s\nPAPER_BUILD=%s\n' "$version" "$build" > "$(dirname "$out")/paper-version.env"
