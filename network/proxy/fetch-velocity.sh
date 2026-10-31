#!/usr/bin/env bash
# Usage: fetch-velocity.sh <velocity-version|latest> <java-version> <output.jar>
set -euo pipefail

version="${1:-latest}"
java="${2:?java version required}"
out="${3:?output path required}"

API="https://fill.papermc.io/v3/projects/velocity"
UA="self-hosted-paper-docker/1.0"

api() { curl -fsSL --retry 5 --retry-delay 3 -A "$UA" "${API}$1"; }

# Velocity marks release builds STABLE or RECOMMENDED; everything else is a snapshot.
stable_build() { api "/versions/$1/builds" | jq -r '[.[] | select(.channel == "STABLE" or .channel == "RECOMMENDED") | .id] | max // empty'; }

if [ "$version" = "latest" ]; then
  for v in $(api "" | jq -r '.versions[][]' | grep -E '^[0-9]+(\.[0-9]+)+$' | sort -rV); do
    if [ -n "$(stable_build "$v")" ]; then version="$v"; break; fi
  done
  [ "$version" != "latest" ] || { echo "No stable Velocity version found" >&2; exit 1; }
fi

build="$(stable_build "$version")"
[ -n "$build" ] || { echo "No stable build for Velocity $version" >&2; exit 1; }

need_java="$(api "/versions/$version" | jq -r '.version.java.version.minimum // 0')"
if [ "$java" -lt "$need_java" ]; then
  echo "Velocity $version needs Java $need_java but the proxy image is being built with Java $java." >&2
  exit 1
fi

api "/versions/$version/builds/$build" > /tmp/velocity-build.json
url="$(jq -r '.downloads["server:default"].url' /tmp/velocity-build.json)"
sha="$(jq -r '.downloads["server:default"].checksums.sha256' /tmp/velocity-build.json)"

mkdir -p "$(dirname "$out")"
echo "Downloading Velocity $version build $build"
curl -fsSL --retry 5 --retry-delay 3 -A "$UA" -o "$out" "$url"
echo "$sha  $out" | sha256sum -c -

printf 'VELOCITY_VERSION=%s\nVELOCITY_BUILD=%s\n' "$version" "$build" > "$(dirname "$out")/velocity-version.env"
