#!/usr/bin/env bash
set -euo pipefail

APP_NAME="McServerGUI"
VERSION="${GITHUB_REF_NAME:-v0.0.0}"
VERSION="${VERSION#v}"
MAIN_JAR="target/McServerGUI.jar"
OUT_DIR="dist"

mkdir -p "$OUT_DIR"

# Stage only the fat JAR so jpackage gets a clean input directory
mkdir -p staging
cp "$MAIN_JAR" staging/

jpackage \
  --type dmg \
  --name "$APP_NAME" \
  --input staging \
  --main-jar "$(basename "$MAIN_JAR")" \
  --dest "$OUT_DIR" \
  --app-version "$VERSION" \
  --vendor "xjustsid" \
  --mac-package-identifier "me.justsid.mcservergui"
