# Detailed test results

## Question under test

Does TCL's private firmware API expose real, changing color or pixel data from
the active HDMI input when the caller has Magisk root or Android system UID?

The answer on the tested TCL 65C725 / R51MT02 configuration was **no**.

## Test matrix

### Pixel statistics

The probe exercised both the public manager wrapper and the private native
entry point:

- `com.tcl.tvmanager.TAppManager.getPixelInfo(...)`
- `com.tcl.tosapi.atv.TvVideoApi.getPixelInfo_native(...)`

Rectangles:

- full 1920×1080 frame;
- full 3840×2160 frame;
- lower-left, lower-right, and center zones.

Native stages:

- `EN_TCL_PIXEL_STAGE_AFTER_DLC`;
- `EN_TCL_PIXEL_STAGE_PRE_GAMMA`;
- `EN_TCL_PIXEL_STAGE_AFTER_OSD`.

`getPixelInfo_native` consistently returned status `0`, but the returned
`ScreenPixelInfo` contained zero for representative color, bounds, min/max
channels, and all RGB sums. The Java manager wrapper behaved identically.

Sanitized representative output:

```text
TCL_PIXEL_NATIVE requestedStage=EN_TCL_PIXEL_STAGE_AFTER_DLC status=0
TCL_PIXEL_ZONE rect=0,0,1920,1080 rep=0 x=0..0 y=0..0
rMin=0 rMax=0 gMin=0 gMax=0 bMin=0 bMax=0
rSum=0 gSum=0 bSum=0
```

### Panel-pixel interface

`tvos.tv.TFactoryManager.getPanelPixel(...)` was called at multiple positions,
including the center and both lower corners. Every call returned `null`.

### TCL snapshot interface

`com.tcl.app.screenshot.ScreenshotImpl.snapshotBuf(...)` was tested with a
1920×1080 source rectangle, a 64×36 output, and formats 0 through 2. Every call
returned `null`.

Relevant native logs showed that the request reached the snapshot service but
the service did not return a buffer:

```text
native_snapshotByte ... resolution [64x36], buffer size 9270
sbinder_snapshot_record_client tos_snapshot_buffer readBlob error
tos_ret:1
```

### HDMI recorder interface

The following private interface was resolved and called successfully at the
Java/JNI boundary:

```text
com.tcl.tosapi.capture.HDMICaptureApi
```

Observed result:

```text
TCL_HDMI_CAPTURE_BEFORE state=-1
TCL_HDMI_CAPTURE_INIT ok=false state=-1
```

No capture file was created.

## Privilege variants

The same probe was executed with:

1. a regular application/shell baseline;
2. Magisk UID 0 with a system `Context` created through `ActivityThread`;
3. UID 1000 (`system`) launched through Magisk.

UID 0 was verified directly and ran in `u:r:magisk:s0`. UID 1000 was also
verified directly. Neither changed the API results.

## SELinux observation

SELinux remained enforcing for all tests. One repeatable denial involved
`tcl_sitatvservice` attempting to signal the Magisk-domain probe process:

```text
avc: denied { signull }
scontext=u:r:tcl_sitatvservice:s0
tcontext=u:r:magisk:s0
tclass=process
```

A temporary, live rule was added for that single operation only:

```text
allow tcl_sitatvservice magisk process signull
```

The denial stopped recurring, while SELinux stayed enforcing. Pixel, snapshot,
and recorder results remained unchanged. No global permissive mode was used.

An attempt to start the probe directly in a TCL service domain was not pursued:
the target domain rejected the executable entry point, and broadening policy or
injecting code into a production TV process was outside the accepted risk.

## AIPQ test

Before the test, TCL reported:

```text
status=false workmode=0
```

The runtime-only call `setVideoAIpqWorkmode(1, 0)` returned `0`. After a delay,
the reported state was still `status=false workmode=0`, and all capture results
remained empty. The original state was explicitly restored and read back.

## HDMI versus Android Home

The complete native probe was first run while a bright external HDMI image was
active. The TV was then switched to Android TV Home and the probe was repeated.
Both runs returned the same zero/`null` results. The original HDMI activity and
input session were restored afterward.

This does not mean HDCP is irrelevant. It means HDCP alone cannot explain why
the TCL snapshot and pixel interfaces also returned no data for Android Home.

## Interpretation

Observed:

- TCL's Java classes and JNI registrations are present;
- calls reach native services;
- the tested caller receives no useful data;
- UID 0 and UID 1000 do not change that behavior;
- the narrow SELinux change does not change that behavior.

Inferred, not proven:

- authorization probably depends on more than Linux/Android UID;
- likely candidates include platform signing, a TCL process identity, an
  internal registration step, or ownership of an active TV input session.

Not tested:

- TCL platform-signed code;
- code injected into `com.tcl.tv`, `com.tcl.tvinput`, or
  `tcl_sitatvservice`;
- direct access to undocumented Realtek driver buffers;
- every firmware version or R51MT02 television model.
