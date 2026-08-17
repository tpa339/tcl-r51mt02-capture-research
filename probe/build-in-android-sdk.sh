#!/bin/sh

set -eu

: "${ANDROID_HOME:?Set ANDROID_HOME to an Android SDK directory}"

android_jar=$(find "${ANDROID_HOME}/platforms" -name android.jar | sort -V | tail -n 1)
build_tools=$(find "${ANDROID_HOME}/build-tools" -mindepth 1 -maxdepth 1 -type d | sort -V | tail -n 1)

test -n "${android_jar}"
test -n "${build_tools}"

mkdir -p build/classes build/dex

javac \
  -source 8 \
  -target 8 \
  -classpath "${android_jar}" \
  -d build/classes \
  src/dev/r51mt02/captureprobe/ProbeActivity.java \
  src/dev/r51mt02/captureprobe/ProbeReceiver.java \
  src/dev/r51mt02/captureprobe/RootProbe.java

jar cf build/classes.jar -C build/classes .

"${build_tools}/d8" \
  --lib "${android_jar}" \
  --output build/dex \
  build/classes.jar

"${build_tools}/aapt2" link \
  -I "${android_jar}" \
  --manifest AndroidManifest.xml \
  --min-sdk-version 26 \
  --target-sdk-version 30 \
  -o build/probe-unsigned.apk

(cd build/dex && zip -q ../probe-unsigned.apk classes.dex)

if [ ! -f build/probe.keystore ]; then
  keytool -genkeypair \
    -keystore build/probe.keystore \
    -storepass android \
    -keypass android \
    -alias probe \
    -dname "CN=TCL Capture Probe, O=Local Test" \
    -keyalg RSA \
    -keysize 2048 \
    -validity 3650 \
    -noprompt
fi

"${build_tools}/zipalign" -f 4 \
  build/probe-unsigned.apk \
  build/probe-aligned.apk

"${build_tools}/apksigner" sign \
  --ks build/probe.keystore \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out build/tcl-capture-probe.apk \
  build/probe-aligned.apk

"${build_tools}/apksigner" verify --verbose build/tcl-capture-probe.apk
