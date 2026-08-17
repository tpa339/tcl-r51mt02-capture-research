#!/bin/sh

set -eu

if [ "$#" -lt 1 ] || [ "$#" -gt 2 ]; then
  printf 'Usage: %s TV_ADDRESS:5555 [baseline|aipq]\n' "$0" >&2
  exit 2
fi

TARGET=$1
MODE=${2:-baseline}
ADB_BIN=${ADB_BIN:-adb}
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROBE_APK="$SCRIPT_DIR/build/tcl-capture-probe.apk"
REMOTE_APK=/data/local/tmp/tcl-capture-probe.apk
REMOTE_CAPTURE=/data/local/tmp/tcl-hdmi-capture-probe.ts
TCL_CLASSPATH="$REMOTE_APK:/product/framework/com.tcl.tvmanager.jar:/product/framework/com.tcl.os.system.jar:/product/framework/com.tcl.deviceinfo.jar:/product/framework/android.tclwidget.jar:/product/framework/com.tcl.media.jar:/system/framework/rtk-framework.jar"

case "$MODE" in
  baseline|aipq) ;;
  *)
    printf 'Unknown mode: %s (expected baseline or aipq)\n' "$MODE" >&2
    exit 2
    ;;
esac

if [ ! -f "$PROBE_APK" ]; then
  printf 'Probe APK missing: %s\n' "$PROBE_APK" >&2
  printf 'Build it with build-in-android-sdk.sh first.\n' >&2
  exit 1
fi

"$ADB_BIN" connect "$TARGET" >/dev/null
if [ "$("$ADB_BIN" -s "$TARGET" get-state 2>/dev/null)" != device ]; then
  printf 'ADB device is not ready: %s\n' "$TARGET" >&2
  exit 1
fi

cleanup() {
  "$ADB_BIN" -s "$TARGET" shell \
    "rm -f '$REMOTE_APK' '$REMOTE_CAPTURE'" >/dev/null 2>&1 || true
}
trap cleanup EXIT INT TERM

if "$ADB_BIN" -s "$TARGET" shell 'command -v su' 2>/dev/null | grep -q '^/'; then
  SU_BIN=su
else
  magisk_version=$("$ADB_BIN" -s "$TARGET" shell \
    '/debug_ramdisk/magisk -V' 2>/dev/null | tr -d '\r' || true)
  if [ -n "$magisk_version" ]; then
    SU_BIN=/debug_ramdisk/su
  else
    printf 'Magisk su binary was not found.\n' >&2
    exit 1
  fi
fi

root_uid=$("$ADB_BIN" -s "$TARGET" shell \
  "$SU_BIN -c 'id -u'" 2>/dev/null | tr -d '\r')
if [ "$root_uid" != 0 ]; then
  printf 'Magisk root was not granted; expected uid 0, got: %s\n' "$root_uid" >&2
  exit 1
fi

"$ADB_BIN" -s "$TARGET" push "$PROBE_APK" "$REMOTE_APK" >/dev/null
ROOT_COMMAND="CLASSPATH=$TCL_CLASSPATH app_process /system/bin dev.r51mt02.captureprobe.RootProbe"
if [ "$MODE" = aipq ]; then
  ROOT_COMMAND="$ROOT_COMMAND aipq"
fi

"$ADB_BIN" -s "$TARGET" shell "$SU_BIN -c '$ROOT_COMMAND'"
