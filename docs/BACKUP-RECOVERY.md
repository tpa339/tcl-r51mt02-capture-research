# Backup and stock recovery

Prepare this before using the root guide. An IMG installation wipes the TV,
and unprivileged ADB cannot make a trustworthy backup of the boot or firmware
partitions on the tested device.

There are therefore two distinct backup tasks:

1. preserve the stock firmware needed to make the TV boot again;
2. record user settings and reinstallable applications.

This repository publishes identifiers and hashes, not copyrighted firmware,
application APKs, credentials, or personal device dumps.

## Recovery firmware set

Keep all three sets before flashing the root image:

### Stock V344 IMG

| File | Bytes | SHA-256 |
|---|---:|---|
| `2023-04-21-V8-R51MT02-LF1V344.7z.001` | 1048576000 | `4f1f7cc7397dc3a91b58135959b1e626feb24edca89a5fc5454ed21622a25f0a` |
| `2023-04-21-V8-R51MT02-LF1V344.7z.002` | 303522461 | `79b7104456b19e75357461fe792f5f19122e5ce8f21dfd6f6cd3e10c3ed7d526` |

Extracted payload:

| Payload | Bytes | SHA-256 |
|---|---:|---|
| Stock V344 `Update.img` | 3491891200 | `82d4f2d52d3040f7f16a1431fc1947bf1b740ec298cdc16f8896a79f88d97fdd` |

### Stock V903 OTA

| File | Bytes | SHA-256 |
|---|---:|---|
| `2024-05-08-V8-R51MT02-LF1V903.7z.001` | 1048576000 | `d42992575cb9497411e58c5d58967580724461658b23a93c6c321796d6380222` |
| `2024-05-08-V8-R51MT02-LF1V903.7z.002` | 664566655 | `7fe701a70dce516de5bd6d9dd736958ea1a300c7aa21173e400d1ae18176fe9d` |

Extracted payload:

| Payload | Bytes | SHA-256 |
|---|---:|---|
| Stock V903 `V8-R51MT02-LF1V903.zip` | 1746048440 | `e5040849777d4fd795e7da1ba798fc9c98d5995ab5102fd6d8aeac34c65efea8` |

Verify the downloaded volumes and extracted payloads with
[`scripts/verify-firmware.sh`](../scripts/verify-firmware.sh). Also run a full
archive test before copying anything to USB.

Keep at least two independently readable copies. A single USB stick is not a
backup. Label the prepared payloads clearly:

- `ROOT-V344`: rooted V344 `Update.img`;
- `STOCK-V344`: stock V344 `Update.img`;
- `STOCK-V903`: untouched stock V903 OTA ZIP.

Never place two different `Update.img` files on the same stick or rely on a
renamed file without checking its hash.

## Settings and application inventory

Photograph or write down the settings that matter before an IMG installation:

- picture mode and per-input picture settings;
- audio output, passthrough, lip-sync, and CEC settings;
- input labels;
- network configuration, without storing the Wi-Fi password in this project;
- developer-options and ADB state;
- settings of any ambient-light or accessibility applications.

Record a non-secret device and package inventory from an authorized ADB
computer:

```sh
adb -s TV_ADDRESS:5555 shell getprop ro.product.model
adb -s TV_ADDRESS:5555 shell getprop ro.build.fingerprint
adb -s TV_ADDRESS:5555 shell pm list packages -3 > third-party-packages.txt
```

Treat the resulting files as local records. Review them before sharing because
installed package names can reveal personal services.

For an application that permits its installed APK files to be read, list every
part with:

```sh
adb -s TV_ADDRESS:5555 shell pm path PACKAGE_NAME
```

Pull every returned path. Split applications require the base APK and all
matching split APKs for reinstallation with `adb install-multiple`. APK copies
do not include application data, login sessions, DRM state, or protected
settings. Android's old `adb backup` mechanism is not a dependable full-device
backup for this TV.

## Normal return to stock

If the rooted V344 system still boots, the least surprising stock path is:

1. verify the stock V903 OTA ZIP again;
2. copy only that untouched ZIP to an empty FAT32 USB stick;
3. insert it into the TV;
4. open **Settings → System Update → Local update**;
5. select the V903 OTA and leave power connected until all reboots finish.

In the observed experiment, a stock V903 OTA installed over rooted V344,
returned the television to a working stock V903/AR10 system, and removed
Magisk. That successful outcome does not guarantee recovery from damaged
partitions.

## Recovery when Android does not boot

Use the stock V344 IMG as the conservative recovery base:

1. verify the stock V344 `Update.img` hash;
2. put only that file on an empty FAT32 USB stick;
3. insert the stick into the TV's USB 2.0 port if available;
4. power the TV off;
5. hold the physical TV power button until the LED blinks, then release it;
6. do not interrupt the IMG update;
7. after V344 boots, apply the verified stock V903 OTA through Local update.

The IMG stage wipes userdata. The subsequent OTA brings the system back to the
recorded V903 build without requiring a modified V903 package.

## Symptom guide

### TV boots, but root has disappeared

Check the software version. If it is stock V903/AR10, an OTA probably replaced
the modified V344 boot image. This is the final state observed in the test and
does not itself require recovery.

### Recovery mode repeats after reboot

The community firmware guide recommends unplugging the TV and waiting ten
minutes before reconnecting power. If it still cannot boot, use the verified
stock V344 IMG procedure above.

### Black screen after an IMG update

Do not flash a different platform. Retry only the matching verified stock
R51MT02 recovery route. The community guide notes that a newer matching OTA can
sometimes recover a black-screen IMG result after the base system is present.

### Update animation appears stuck

Keep power stable while there is visible update activity. The green bars on
the blue screen are a looping animation and do not represent a percentage.
Allow at least 30 minutes before treating the run as abnormal. If the updater
never finishes, record the exact screen and LED behavior before attempting the
verified stock V344 IMG; repeated power cycling during writes increases risk.

### Stock V344 recovery also fails

Stop trying random firmware. Record the exact model, complete software string,
screen/LED behavior, file hashes, and steps already attempted. Seek
model-specific help through the linked TCL/XDA/4PDA communities or a repair
service capable of board-level recovery.

## Recovery sources

- [TCL firmware identification, IMG/OTA installation, and troubleshooting](https://github.com/mojothemonkey2/TCL_Guide#firmware-updates)
- [R51MT02 community firmware post](https://4pda.to/forum/index.php?showtopic=1024588&st=52460)
- [Stock R51MT02 IMG collection](https://disk.yandex.ru/d/7ezQlN9aXR0Sbg)
- [Stock R51MT02 OTA collection](https://disk.yandex.ru/d/gOuLfHvlo1v4lg)

These are community resources, not an official TCL recovery service. File
availability can change; the hashes in this repository describe the exact
objects tested in this experiment.
