# Reproduction probe

This small Android probe calls the private TCL interfaces documented in the
repository root. It records only returned numeric values and short byte-buffer
metadata. It does not transmit captured data.

## Requirements

- a TCL firmware exposing the referenced framework classes;
- Android SDK platform and build-tools;
- JDK 17;
- ADB access;
- for `run-root-probe.sh`, an already rooted television with Magisk.

No TCL framework JAR is bundled. At runtime, the root runner loads the copies
already present on the television.

## Build

```sh
cd probe
ANDROID_HOME=/path/to/android-sdk \
JAVA_HOME=/path/to/jdk-17 \
sh build-in-android-sdk.sh
```

The generated test APK and local debug keystore are written under `build/`,
which is excluded from Git.

## Regular application baseline

```sh
adb connect TV_ADDRESS:5555
adb install -r build/tcl-capture-probe.apk
adb logcat -c
adb shell am broadcast \
  -a dev.r51mt02.captureprobe.PROBE \
  -n dev.r51mt02.captureprobe/.ProbeReceiver
adb logcat -d -s TclCaptureProbe
adb uninstall dev.r51mt02.captureprobe
```

## Root runner

```sh
./run-root-probe.sh TV_ADDRESS:5555 baseline
```

The optional `aipq` mode temporarily requests AIPQ `ON` with TCL's runtime-only
`EXEC` control, runs the probe, and restores the previous work mode in a
`finally` block:

```sh
./run-root-probe.sh TV_ADDRESS:5555 aipq
```

The runner checks for real UID 0 before execution and removes its temporary APK
and capture file when it exits. Review the source before running it. Do not use
the AIPQ mode on an unsupported model.
