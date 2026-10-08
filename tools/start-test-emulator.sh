#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android}}"
avd_name="${1:-freeotp_shortcuts_test}"
if (( $# )); then shift; fi

# avdmanager may use the XDG directory while emulator defaults to ~/.android.
if [[ -z "${ANDROID_AVD_HOME:-}" && -d "${XDG_CONFIG_HOME:-$HOME/.config}/.android/avd" ]]; then
    export ANDROID_AVD_HOME="${XDG_CONFIG_HOME:-$HOME/.config}/.android/avd"
fi

# SwiftShader GLES crashes on this Linux host. Use Mesa's host OpenGL driver.
exec "$sdk_root/emulator/emulator" -avd "$avd_name" \
    -no-window -no-audio -no-boot-anim -no-snapshot \
    -gpu "${EMULATOR_GPU:-host}" -feature -Vulkan -memory 2048 -cores 2 "$@"
