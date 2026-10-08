[![Build Status](https://github.com/mstsirkin/freeotp/actions/workflows/build.yml/badge.svg?branch=freeotp-plus)](https://github.com/mstsirkin/freeotp/actions/workflows/build.yml)

# FreeOTP Plus

FreeOTP Plus is a fork of [FreeOTP for Android](https://github.com/freeotp/freeotp-android),
with account automation, Bluetooth keyboard sharing, and an HOTP backup fix.
It is a two-factor authentication application for systems utilizing one-time password
protocols. Tokens can be added easily by scanning a QR code.

This fork lives at [mstsirkin/freeotp](https://github.com/mstsirkin/freeotp), on the
`freeotp-plus` branch. It is separate from the [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus) project.

FreeOTP implements open standards:

* HOTP (HMAC-Based One-Time Password Algorithm) [RFC 4226](https://www.ietf.org/rfc/rfc4226.txt)
* TOTP (Time-Based One-Time Password Algorithm) [RFC 6238](https://www.ietf.org/rfc/rfc6238.txt)

This means that no proprietary server-side component is necessary: use any server-side component that implements these standards.

## Screenshots
<img src="screenshots/1.png" alt="FreeOTP screenshot 1" width="200" /> &nbsp;<img src="screenshots/2.png" alt="FreeOTP screenshot 2" width="200" /> &nbsp;<img src="screenshots/3.png" alt="FreeOTP screenshot 3" width="200" /> &nbsp;<img src="screenshots/4.png" alt="FreeOTP screenshot 4" width="200" />

## Download upstream FreeOTP for Android

These store links install upstream FreeOTP; they do not include this fork's additions.

* [F-Droid](https://f-droid.org/packages/org.fedorahosted.freeotp)
* [Google Play](https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp)

<a href="https://f-droid.org/packages/org.fedorahosted.freeotp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60"/></a>
<a href="https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="60"/></a>

## Contributing

Pull requests on GitHub are welcome under the Apache 2.0 license, see [CONTRIBUTING](CONTRIBUTING.md) for more details.

## Permissions

FreeOTP Plus uses the following permissions, depending on the features you enable:

| Permission | Usage                    | Required | Permission type |
|------------|--------------------------|----------|-----------------|
| Camera     | Recognition of QR codes  | No       | Dangerous       |
| Internet   | Token image provisioning | No       | Normal          |
| Bluetooth / Nearby devices | Discover, pair, and connect sharing destinations | No | Depends on Android version |
| Location | Bluetooth discovery on older Android versions | No | Dangerous |

## Alternatives

Here are some open-source alternative apps providing similar functionality:
- [Aegis](https://github.com/beemdevelopment/Aegis)
- [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus)
- [Proton Authenticator](https://github.com/protonpass/android-authenticator)

## Features added in this fork

### Account information

Select one account and use the information action in the selection toolbar to view
its issuer, account name, token type, algorithm, code length, and authentication
requirement. TOTP accounts also show their period; HOTP accounts show the next counter.
Viewing these properties does not generate a code or advance the counter.

### Per-account startup generation and automatic sharing

Each account has a compact row of controls:

* **Robot:** generate a code when the app starts.
* **Keyboard:** automatically type newly generated codes on the selected Bluetooth destination.
* **Clipboard:** automatically copy newly generated codes.
* **Antenna (Jelling):** automatically send newly generated codes to a Jelling receiver.

Choose one sharing destination per account, or none. Tap the selected destination
to clear it. A selected destination applies both when you tap the account to generate
a code and when it generates a code at startup. The Share button reuses that destination.
Changing these controls never generates a code or advances HOTP.

Manual accounts may use the same sharing destination. Among accounts enabled for
startup generation, each destination can belong to only one account. Selecting a
conflicting configuration keeps the latest choice and disables startup generation
on the other account, preserving its destination and showing an explanation.
Multiple accounts without a sharing destination may generate at startup.

Startup generation runs once on a fresh activity launch, rather than every return
from settings or sharing. Protected accounts still require authentication. Clipboard
startup delivery runs first; keyboard and Jelling sessions are queued. Expired queued
codes are discarded without generating replacements. Automatic sends are not retried.

### Sharing settings and direct sharing

Open **Advanced settings** from the main menu to enable or disable Clipboard,
Jelling, and Bluetooth keyboard independently. All three are enabled by default;
keyboard sharing requires Android 9 or later. Disabled methods do not request their
transport permissions. A per-account destination must also be enabled here to send.

For accounts without a selected destination, Share uses the enabled methods:

* One enabled method sends directly, bypassing the chooser.
* Multiple enabled methods show the chooser.
* No enabled methods opens Advanced settings.

Direct Jelling sharing searches for receivers for six seconds, sends automatically
when one is found, and offers a choice when several are found. Keep the receiver
running on your computer. The legacy global Auto Clipboard setting is removed;
automatic clipboard copying is now configured per account.

### Bluetooth keyboard sharing

**Send as keyboard** types the displayed code on a paired computer without a
companion Android app. **Manage keyboard destinations** in Advanced settings lets
you select a remembered destination or pair a new one by finding a visible computer
or making the phone visible. The device screen also offers accessory filtering,
Show all devices, and diagnostics.

Sending opens a foreground keyboard screen and uses the selected destination.
If none is selected, select or pair one first. Sharing an existing code does not
regenerate it or advance HOTP. Keep the keyboard screen visible during sending.

**Enter after keyboard send** is enabled by default for new and existing
installations. Turn it off in Advanced settings if the receiving application should
not submit immediately. Enter is sent only after the complete code; a failed or
interrupted code does not send the trailing Enter. Clipboard and Jelling are unaffected.

Numbers are normally unaffected by keyboard language changes; letters and symbols
require a US English layout with Caps Lock off. Linux must authorize keyboard access
for incoming reconnections. Failed sends preserve the code for manual retry. Check
for partial input before retrying; partial sends are not retried automatically.
Physical phone/computer testing remains required for this integration.

If you use your phone as a Bluetooth keyboard but do not want calls or media audio
routed to the computer, first check your phone's per-device Bluetooth settings.
On Redmi, go to **Bluetooth → More settings → right arrow beside the computer**,
then turn off **Call audio** and **Media audio**. The phone stays paired for keyboard
use, while audio stays on the phone. Settings vary by phone.

If those settings aren't available, or you want to enforce the restriction from
the computer, see [btkeyboardonly](https://github.com/mstsirkin/btkeyboardonly)
for Linux setup instructions. With BlueZ and Blueman, trusting the entire phone can
also allow its audio services and route sound to the computer. `btkeyboardonly`
automatically authorizes incoming classic Bluetooth HID connections from selected
paired devices while rejecting their other service authorization requests, so you
can allow keyboard reconnections without whole-device trust.

### FreeOTP Plus branding

App names, welcome screens, backup prompts, About text, and launcher and in-app
logos identify this fork as FreeOTP Plus. Upstream attribution and links are retained.

## Bugfixes

### HOTP backup counters stay synchronized

Upstream backup metadata could retain an older HOTP counter after codes were
generated. Restoring such a backup could restore a stale counter and produce codes
that the server had already consumed.

This fork updates backup metadata when the live HOTP counter advances and refreshes
metadata from the current accounts before exporting a backup. Export also repairs
stale metadata left by older versions, without changing the encrypted secrets.
Previously exported backup files are unchanged; export a new backup to capture the
current counters.

**Restoring an upstream FreeOTP backup:** HOTP backups made by classic upstream
FreeOTP have broken counter metadata. If you restore affected HOTP accounts from
such a backup, you must re-register those accounts with their services. Restoring
the old backup in FreeOTP Plus cannot recover the missing counter updates. TOTP
backups work normally and do not require re-registration because of this bug.

**Restoring a FreeOTP Plus backup:** backups made by this fork preserve the current
HOTP counters and can be restored in either FreeOTP Plus or upstream FreeOTP. The
backup format remains compatible. If you subsequently generate HOTP codes in
upstream FreeOTP, its backup-counter bug still applies to backups it creates.

## Release tags

Tags for this fork use `plus-<upstream-version>-<fork-revision>`, for example
`plus-2.0.6-7` for app version `2.0.6-plus.7`. The `plus-` prefix distinguishes
this fork's releases from upstream tags.

## Personal release build

The personal release is built with
`./gradlew -I tools/personal-release.gradle :mobile:assembleRelease`, with the
original signing key supplied through `TXT_BT_SIGNING_KEY`. It retains the personal
app ID (`org.fedorahosted.freeotp.debug`) and signing identity so it updates that
installation without clearing accounts. The release is not debuggable; the `.debug`
suffix preserves installation identity. Keep release outputs in a separate checkout
to preserve debug artifacts.
