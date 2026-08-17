# Root boundary and recovery notes

This document records what was actually tested. It is not a rooting guide.

## Root that was achieved

The television booted a minimally modified V344 image with Magisk 29. The
verified runtime state was:

- firmware build `AR06`;
- SELinux enforcing;
- Magisk daemon running;
- UID 0 available through `/debug_ramdisk/su`;
- no conventional working `/system/bin/su` path.

The probe verified root with `id` before invoking any private TCL method. A
second run used UID 1000 through Magisk. Both returned the same empty capture
results.

## What the root result does and does not show

It shows that a genuine root caller with a system context could load the TCL
framework and reach its native methods.

It does not show that the caller had TCL's platform signature, the SELinux
domain of an existing TV service, or ownership of the active input session.
Those are distinct security and architecture boundaries.

## Automatic OTA outcome

After a later reboot, TCL's updater automatically installed stock V903
(`AR10`). The update completed successfully and the television remained
functional, but the modified boot image and Magisk root were removed.

Researchers should assume that an accepted stock OTA can overwrite a rooted
boot image. Do not rely on root surviving an update, and do not experiment
without a model-specific, independently verified recovery path.

## Why the experiment stopped

All lower-risk privilege and API variants had already failed. The remaining
ideas required SELinux domain expansion, process injection, or undocumented
driver access. Those steps could destabilize core TV services and still had no
evidence-backed path to HDMI pixels. The investigation therefore stopped with
the television restored to a working stock state.

No firmware files, signing material, APK backups, or vendor framework binaries
are published in this repository.
