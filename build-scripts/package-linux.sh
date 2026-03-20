#!/usr/bin/env bash
set -euo pipefail

APP_NAME="mcservergui"
VERSION="${GITHUB_REF_NAME:-v0.0.0}"
VERSION="${VERSION#v}"
MAIN_JAR="target/McServerGUI.jar"
OUT_DIR="dist"

mkdir -p "$OUT_DIR"

# Try deb first (Debian/Ubuntu), then rpm (Fedora/RHEL).
if jpackage \
  --type deb \
  --name "$APP_NAME" \
  --input target \
  --main-jar "$(basename "$MAIN_JAR")" \
  --dest "$OUT_DIR" \
  --app-version "$VERSION" \
  --vendor "xjustsid"; then
  echo "Created .deb package"
else
  echo "deb packaging failed, trying rpm"
  jpackage \
    --type rpm \
    --name "$APP_NAME" \
    --input target \
    --main-jar "$(basename "$MAIN_JAR")" \
    --dest "$OUT_DIR" \
    --app-version "$VERSION" \
    --vendor "xjustsid"
fi
