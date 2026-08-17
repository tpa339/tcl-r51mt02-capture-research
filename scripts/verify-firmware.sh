#!/bin/sh

set -eu

if [ "$#" -eq 0 ]; then
  printf 'Usage: %s FILE [FILE ...]\n' "$0" >&2
  exit 2
fi

file_size() {
  if stat -f%z "$1" >/dev/null 2>&1; then
    stat -f%z "$1"
  else
    stat -c%s "$1"
  fi
}

sha256() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1" | awk '{print $1}'
  else
    shasum -a 256 "$1" | awk '{print $1}'
  fi
}

expected_for() {
  case "$1" in
    2023-04-21-V8-R51MT02-LF1V344.7z.001)
      printf '%s %s\n' 1048576000 4f1f7cc7397dc3a91b58135959b1e626feb24edca89a5fc5454ed21622a25f0a
      ;;
    2023-04-21-V8-R51MT02-LF1V344.7z.002)
      printf '%s %s\n' 303522461 79b7104456b19e75357461fe792f5f19122e5ce8f21dfd6f6cd3e10c3ed7d526
      ;;
    2024-05-08-V8-R51MT02-LF1V903.7z.001)
      printf '%s %s\n' 1048576000 d42992575cb9497411e58c5d58967580724461658b23a93c6c321796d6380222
      ;;
    2024-05-08-V8-R51MT02-LF1V903.7z.002)
      printf '%s %s\n' 664566655 7fe701a70dce516de5bd6d9dd736958ea1a300c7aa21173e400d1ae18176fe9d
      ;;
    2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.001)
      printf '%s %s\n' 1048576000 c21328d1d84e8d44e1054c4d61f43adb1fe995325cb75acbedf6419c747a1639
      ;;
    2023-04-21-V8-R51MT02-LF1V344_Root_Magisk29.7z.002)
      printf '%s %s\n' 324343560 d2ce799efce2225f65bcaa8c5f1556458aad994d2470e8d1e3406c27a12ac3c2
      ;;
    V8-R51MT02-LF1V903.zip)
      printf '%s %s\n' 1746048440 e5040849777d4fd795e7da1ba798fc9c98d5995ab5102fd6d8aeac34c65efea8
      ;;
    Update.img)
      # The rooted and stock payloads share this basename. Validation happens
      # against both complete size/hash pairs below.
      printf 'ambiguous\n'
      ;;
    *)
      return 1
      ;;
  esac
}

failures=0

for path in "$@"; do
  if [ ! -f "$path" ]; then
    printf 'MISSING  %s\n' "$path"
    failures=$((failures + 1))
    continue
  fi

  name=${path##*/}
  if ! expected=$(expected_for "$name"); then
    printf 'UNKNOWN  %s\n' "$path"
    failures=$((failures + 1))
    continue
  fi

  actual_size=$(file_size "$path")
  actual_hash=$(sha256 "$path")

  if [ "$expected" = ambiguous ]; then
    if [ "$actual_size" = 3491891200 ] && \
       [ "$actual_hash" = 82d4f2d52d3040f7f16a1431fc1947bf1b740ec298cdc16f8896a79f88d97fdd ]; then
      printf 'OK       %s (stock V344)\n' "$path"
    elif [ "$actual_size" = 3491891200 ] && \
         [ "$actual_hash" = f0a4c269f6590168b4d855f61700c2f4ddcf613645995e63d1a6cbf587635358 ]; then
      printf 'OK       %s (root V344)\n' "$path"
    else
      printf 'FAILED   %s (unknown Update.img)\n' "$path"
      printf '         size: %s\n' "$actual_size"
      printf '         sha256: %s\n' "$actual_hash"
      failures=$((failures + 1))
    fi
    continue
  fi

  expected_size=${expected%% *}
  expected_hash=${expected#* }
  if [ "$actual_size" = "$expected_size" ] && [ "$actual_hash" = "$expected_hash" ]; then
    printf 'OK       %s\n' "$path"
  else
    printf 'FAILED   %s\n' "$path"
    printf '         size: expected %s, got %s\n' "$expected_size" "$actual_size"
    printf '         sha256: expected %s, got %s\n' "$expected_hash" "$actual_hash"
    failures=$((failures + 1))
  fi
done

if [ "$failures" -ne 0 ]; then
  printf '\nNO-GO: %s file check(s) failed. Do not flash.\n' "$failures"
  exit 1
fi

printf '\nAll supplied files match the recorded test set.\n'
