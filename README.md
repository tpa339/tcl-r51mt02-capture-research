# TCL R51MT02 native HDMI capture research

Root access was successfully obtained on a TCL R51MT02 television, but every
tested private pixel, screenshot, and HDMI-capture interface still returned
empty data. This repository documents that negative result so others can avoid
repeating the same risky firmware experiment.

## Bottom line

On the tested TCL 65C725, **Magisk root alone was not sufficient to read pixel
or color data from an HDMI input**.

The native methods exist and can be invoked. They do not throw a Java
permission error. Nevertheless, calls made as root, as Android UID 1000, and
with a system context returned only zero-filled structures, `null`, or an
initialization failure.

This does not prove that capture is impossible on every R51MT02 build. It does
show that rooting the TV and calling TCL's private APIs from an external
process is not a working solution on the configuration below.

## Tested configuration

| Component | Value |
|---|---|
| Television | TCL 65C725 |
| Platform | R51MT02 / `BeyondTV4` / `BeyondTV6` |
| Android | Android TV 11, SDK 30 |
| Root test firmware | V344, build `AR06` |
| Root implementation | Magisk 29 |
| Stock firmware restored by OTA | V903, build `AR10` |
| SELinux during tests | Enforcing |
| Users tested | UID 0 (`root`) and UID 1000 (`system`) |

Root on the V344 image was exposed through `/debug_ramdisk/su`; a conventional
`/system/bin/su` path was not available. A later automatic stock OTA restored
V903 and removed root. No claim is made here that V903 itself was tested with
root.

## Results at a glance

| Interface | Observed result |
|---|---|
| `TAppManager.getPixelInfo(...)` | All fields zero |
| `TvVideoApi.getPixelInfo_native(...)` | Status `0`, all fields zero |
| `TFactoryManager.getPanelPixel(...)` | `null` |
| `ScreenshotImpl.snapshotBuf(...)` | `null` for formats 0, 1, and 2 |
| `HDMICaptureApi.getState()` | `-1` |
| `HDMICaptureApi.init(...)` | `false` |
| Temporary AIPQ enable request | Returned `0`, but AIPQ remained off |

The pixel probe covered 1920×1080 and 3840×2160 rectangles and the
`AFTER_DLC`, `PRE_GAMMA`, and `AFTER_OSD` stages. The same empty result was
observed on a bright external HDMI image and after switching to Android TV
Home. That differential test makes HDCP an insufficient explanation for the
failure, although protected HDMI content remains an additional constraint.

See [docs/RESULTS.md](docs/RESULTS.md) for the detailed evidence and
[docs/ROOT-NOTES.md](docs/ROOT-NOTES.md) for the exact root boundary that was
tested.

## Why repeated Android screenshots are not a workaround

Android UI and external HDMI video do not necessarily travel through the same
compositor path. The HDMI image on this platform is handled as a hardware video
plane. Repeated `screencap` or MediaProjection frames therefore do not provide
the external HDMI pixels needed for real-time color extraction. Raising the
capture frequency to 25 frames per second only adds load; it does not change
which plane is visible to the caller.

## Most likely remaining boundary

The evidence is consistent with an internal authorization, process-domain, or
active-session check below the public Java layer. TCL services were reached,
but they did not return usable buffers to the external caller. Root UID alone,
Android system UID alone, and one narrowly targeted SELinux rule did not change
the result.

A future investigation would need one of the following:

- an API or allowlist supplied by TCL;
- code signed with the relevant TCL platform key;
- carefully audited execution inside an already authorized TCL process; or
- an external HDMI capture path.

Process injection was deliberately not attempted. Its risk was not justified
after all lower-risk probes failed.

## Probe source

The [`probe/`](probe/) directory contains the original reflection-based test
code in a neutral package namespace. It references TCL framework class names
but includes no TCL firmware, framework JAR, APK, key, or other vendor binary.

The code is published for reproducibility, not as a supported application.
Only run it on hardware you own and only after making an independent recovery
plan. The probe does not enable root and this repository does not distribute or
recommend modified firmware.

## Scope and privacy

No private IP addresses, serial numbers, device tokens, account information,
firmware archives, application backups, or captured media are included.

This project is independent research and is not affiliated with or endorsed by
TCL, Google, or Magisk.
