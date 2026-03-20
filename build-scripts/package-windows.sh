#!/usr/bin/env bash
set -euo pipefail

APP_NAME="McServerGUI"
VERSION="${GITHUB_REF_NAME:-v0.0.0}"
VERSION="${VERSION#v}"
MAIN_JAR="target/McServerGUI.jar"
OUT_DIR="dist"

mkdir -p "$OUT_DIR"

jpackage \
  --type msi \
  --name "$APP_NAME" \
  --input target \
  --main-jar "$(basename "$MAIN_JAR")" \
  --dest "$OUT_DIR" \
  --app-version "$VERSION" \
  --vendor "xjustsid" \
  --win-shortcut \
  --win-menu
