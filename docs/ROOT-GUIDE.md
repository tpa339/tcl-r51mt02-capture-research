# Root guide: TCL 65C725 / R51MT02 / V344

This is the exact route that produced verified UID 0 on one TCL 65C725. It is
not a generic TCL rooting method.

## Compatibility boundary

Tested successfully:

- television: TCL 65C725;
- platform string: `V8-R51MT02-LF1`;
- root image base: V344, build `AR06`;
- Android TV 11;
- Magisk 29 contained in the third-party root image.

Do not proceed merely because the television looks similar. Check **Settings →
Device Preferences → About → Product Information** first. The software string
must contain `R51MT02`, character for character. Do not use R51MT01, R51MT05,
or another platform image.

The firmware author's original post explicitly warns that this V344 image
predates the P745 and must not be installed on that model. Other R51MT02 models
remain unverified by this repository.

## Consequences and known risks

- An IMG installation erases the TV and its settings.
- This RT51 device is single-slot; there is no inactive A/B slot to rescue a
  failed boot.
- The bootloader remained locked and Verified Boot remained enforcing in the
  tested setup. This method uses a complete third-party IMG installer rather
  than a normal unlocked-bootloader workflow.
- Root can affect DRM, app certification, security, warranty, and future
  updates.
- The tested TV later installed stock V903 automatically and removed root,
  despite update prompts having been declined earlier. There is no proven
  update-blocking procedure in this repository.
- A power loss while the IMG is writing can leave the TV unbootable.

Prepare the stock recovery material in
[BACKUP-RECOVERY.md](BACKUP-RECOVERY.md) before installing anything.

## Files used in the successful test

The repository does not redistribute firmware. The file names below identify
the exact public objects that were downloaded and tested.

### V344 root image archive

| File | Bytes | SHA-256 |
|---|---:|---|
| `2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.001` | 1048576000 | `c21328d1d84e8d44e1054c4d61f43adb1fe995325cb75acbedf6419c747a1639` |
| `2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.002` | 324343560 | `d2ce799efce2225f65bcaa8c5f1556458aad994d2470e8d1e3406c27a12ac3c2` |

After extraction, the payload used was:

| Payload | Bytes | SHA-256 |
|---|---:|---|
| Root-only V344 `Update.img` | 3491891200 | `f0a4c269f6590168b4d855f61700c2f4ddcf613645995e63d1a6cbf587635358` |

Run the repository verifier against both split volumes and the extracted
payload:

```sh
./scripts/verify-firmware.sh \
  /path/to/2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.001 \
  /path/to/2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.002 \
  /path/to/Update.img
```

The basename `Update.img` is ambiguous between stock and rooted images. The
verifier therefore accepts it only when its hash matches one of the recorded
payloads. Any mismatch is a stop condition.

Test the complete split archive before extracting it. With the official 7-Zip
CLI this is:

```sh
7zz t 2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.001
```

## What was audited before flashing

The extracted V344 root image was recursively compared with the stock V344
image. The partition payloads for bootloader, recovery, DTBO, vbmeta, system,
product, vendor, TCL configuration, and the userdata template were identical.
The differences were limited to `boot.img` and the installer resources.

The added ramdisk contained Magisk 29 components matching the official Magisk
29 APK. The kernel command line changed `skip_initramfs` to `want_initramfs`.
The installer-bundled recovery executable also differed by nine instruction
bytes, while the actual `recovery.img` partition payload remained stock. Those
nine bytes are still unaudited third-party installer code and remain the main
supply-chain risk.

## USB preparation

1. Format a reliable USB stick as FAT32. A USB 2.0 port is recommended.
2. Make the stick empty.
3. Copy only the verified rooted V344 `Update.img` to its root directory.
4. Eject the stick cleanly, reconnect it to the computer, and hash the copy on
   the stick again.
5. Keep independently verified stock V344 and V903 recovery files available
   on another tested medium or computer before continuing.

Do not put the split `.7z.001`/`.002` files on the TV stick. The TV needs the
extracted `Update.img`.

## Installing the rooted V344 IMG

1. Disconnect Ethernet and do not configure Wi-Fi during the initial rooted
   setup. This reduces the chance of an immediate stock OTA replacing root.
2. Turn the TV off.
3. Insert the prepared USB stick.
4. Press and hold the **physical power button on the television**, not the
   remote-control power button.
5. Keep holding until the status LED starts blinking, then release it.
6. The blue update screen should appear. Green bars repeatedly building from
   left to right are the normal progress animation.
7. Do not remove power or USB while the updater is active. The successful run
   took long enough that a 30-minute wait was reasonable.
8. After the installer completes, allow the TV to reboot through the TCL and
   Android animations. The first boot can take longer than normal.
9. Complete only the minimum initial setup and remain offline until root has
   been verified.

If the display remains unchanged for an extended period or the TV no longer
boots, do not experiment with unrelated firmware. Follow
[BACKUP-RECOVERY.md](BACKUP-RECOVERY.md).

## Verifying root

Enable developer options again because the IMG installation resets the TV:

1. Open **Settings → Device Preferences → About**.
2. Press OK seven times on **Android TV OS build**.
3. Enable **USB debugging** in Developer options.
4. Find the TV's current IP address and connect from a computer:

```sh
adb connect TV_ADDRESS:5555
adb -s TV_ADDRESS:5555 shell /debug_ramdisk/magisk -V
adb -s TV_ADDRESS:5555 shell /debug_ramdisk/su -c id
```

Accept the debugging authorization dialog on the TV. The last command must
report `uid=0`. On the tested image the usable binary was
`/debug_ramdisk/su`; `/system/bin/su` was not a working substitute.

The verified runtime state was Magisk 29 with SELinux still enforcing. Do not
upgrade Magisk or install modules before recording a clean baseline. They were
not required for the successful root verification and add new failure modes.

## After verification

Root is confirmed only when the `id` command returns UID 0. A Magisk icon or
manager screen alone is not proof.

The capture probe can then be built and run as described in
[`probe/README.md`](../probe/README.md). In this experiment, root worked but
all tested TCL capture APIs still returned empty data.

## Sources

- [Community R51MT02 root-firmware post](https://4pda.to/forum/index.php?showtopic=1024588&st=52460)
- [TCL firmware identification and USB IMG/OTA procedure](https://github.com/mojothemonkey2/TCL_Guide#firmware-updates)
- [Public firmware collection](https://disk.yandex.ru/d/tbWg1XhbKTgM6A)
- [Stock R51MT02 IMG collection](https://disk.yandex.ru/d/7ezQlN9aXR0Sbg)
- [Stock R51MT02 OTA collection](https://disk.yandex.ru/d/gOuLfHvlo1v4lg)
- [Official Magisk releases](https://github.com/topjohnwu/Magisk/releases)

Community firmware and file hosts are not authoritative TCL support channels.
Availability and content can change. Verify every downloaded byte locally.
