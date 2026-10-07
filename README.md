[![Build Status](https://github.com/freeotp/freeotp-android/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/freeotp/freeotp-android/actions/workflows/build.yml)

# FreeOTP

[FreeOTP](https://freeotp.github.io) is a two-factor authentication application for systems
utilizing one-time password protocols. Tokens can be added easily by scanning a QR code.

FreeOTP implements open standards:

* HOTP (HMAC-Based One-Time Password Algorithm) [RFC 4226](https://www.ietf.org/rfc/rfc4226.txt)
* TOTP (Time-Based One-Time Password Algorithm) [RFC 6238](https://www.ietf.org/rfc/rfc6238.txt)

This means that no proprietary server-side component is necessary: use any server-side component that implements these standards.

## Screenshots
<img src="screenshots/1.png" alt="FreeOTP screenshot 1" width="200" /> &nbsp;<img src="screenshots/2.png" alt="FreeOTP screenshot 2" width="200" /> &nbsp;<img src="screenshots/3.png" alt="FreeOTP screenshot 3" width="200" /> &nbsp;<img src="screenshots/4.png" alt="FreeOTP screenshot 4" width="200" />

## Download FreeOTP for Android

* [F-Droid](https://f-droid.org/packages/org.fedorahosted.freeotp)
* [Google Play](https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp)

<a href="https://f-droid.org/packages/org.fedorahosted.freeotp/" target="_blank">
<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="60"/></a>
<a href="https://play.google.com/store/apps/details?id=org.fedorahosted.freeotp" target="_blank">
<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png" alt="Get it on Google Play" height="60"/></a>

## Contributing

Pull requests on GitHub are welcome under the Apache 2.0 license, see [CONTRIBUTING](CONTRIBUTING.md) for more details.

## Permissions

The FreeOTP app uses the following permissions

| Permission | Usage                    | Required | Permission type |
|------------|--------------------------|----------|-----------------|
| Camera     | Recognition of QR codes  | No       | Dangerous       |
| Internet   | Token image provisioning | No       | Normal          |

## Alternatives

Here are some open-source alternative apps providing similar functionality:
- [Aegis](https://github.com/beemdevelopment/Aegis)
- [FreeOTP+](https://github.com/helloworld1/FreeOTPPlus)
- [Proton Authenticator](https://github.com/protonpass/android-authenticator)

## FreeOTP Plus Bluetooth sharing

The share panel offers Clipboard, Jelling, and **Send as keyboard**. **Sharing settings** in the burger menu enables or disables clipboard, Jelling, and Bluetooth keyboard independently. All three sharing methods are enabled by default, with keyboard hidden on Android versions before 9. Disabled methods are not constructed and do not request their transport permissions.

**Manage keyboard destinations** opens the device screen reused from TXT to BT: select a remembered destination, add a device by finding a visible computer or making the phone visible, and filter known accessories. Show all devices and diagnostics are in the overflow menu. Keyboard sharing launches this private foreground screen with the displayed code and sends once to the selected device; without a destination, select or pair one first. It does not regenerate a code or advance HOTP. The keyboard classes are built into FreeOTP Plus; no companion Android app is needed.

Keep the keyboard screen visible during sending. Numbers are normally unaffected by letter-language changes, while letters and symbols require US English with Caps Lock off. Linux must authorize keyboard access and trust the phone for incoming reconnections. Failures preserve the code for manual retry; partial sends are not retried automatically.

The APK also includes the earlier HOTP backup-counter fix (separate commit). Physical phone/computer testing remains required for this integration. The standalone keyboard regression checks can be compiled with KeyboardCodec.java and KeyboardReports.java and run as KeyboardCodecTest / KeyboardReportsTest.

### Direct sharing (2.0.6-plus.2)

When exactly one method is enabled, Share bypasses the transport chooser: clipboard copies immediately; keyboard opens its foreground session and sends to the remembered destination; Jelling immediately starts receiver discovery, automatically sending when one receiver is available after a six-second discovery window, with a receiver choice if multiple are found. No methods enabled opens Sharing settings instead of an empty chooser. The clipboard share switch is independent of the existing automatic clipboard-copy setting. Jelling does not automatically resend after a send has begun, including fragment recreation or a reported failure.

### Account automation (2.0.6-plus.3)

Sharing settings now has switches for **Generate codes on startup** and **Auto-share generated codes**. Both are off by default (the existing startup-generation preference is preserved when updating). Enabling either switch shows its checkbox and icon above every account: lightning for startup generation and an arrow for sharing. Any number of accounts can generate at startup; only one account can auto-share. Selecting a new sharing account deselects the previous one; tapping the selected checkbox clears it. Disabling a global switch hides its controls and stops that behavior while remembering selections.

Auto-share uses the existing enabled sharing methods whenever that account actually generates a code. To send on startup, select both checkboxes for that account. One enabled method sends directly; multiple methods still show the chooser. Checkbox changes do not generate codes or advance HOTP. Protected accounts retain their normal authentication requirement. Startup runs once on a fresh activity launch, not on returning from settings or sharing; no automatic resend or retry occurs. Account selections use stable UUIDs and are cleared when the selected account is deleted.

### Branding and personal release (2.0.6-plus.4)

All translated app-facing names, welcome screens, backup prompts and About text use FreeOTP Plus. The in-app logo carries the same plus badge as the adaptive and legacy launcher icons. Upstream attribution and links are retained.

The personal release is built with `-I tools/personal-release.gradle :mobile:assembleRelease` and the original key supplied through `TXT_BT_SIGNING_KEY`. It retains the installed personal app ID (`org.fedorahosted.freeotp.debug`) and signing identity so it updates that app without clearing accounts. The release is not debuggable; the `.debug` suffix preserves installation identity. Keep release outputs in a separate checkout to preserve debug artifacts.

### Sharing menu cleanup (2.0.6-plus.5)

The burger menu labels Sharing settings with a gear and navigation chevron. The legacy global Auto Clipboard entry and automatic-copy behavior are removed, including when an older installation saved that preference as enabled. Clipboard remains a sharing destination controlled in Sharing settings; per-account auto-share uses that same destination configuration.

The settings entry is named Advanced settings with a right-pointing triangle; its back button uses the matching left-pointing triangle. Per-account controls have full text labels, a robot emoji for startup generation and the existing share graphic for automatic sharing, tinted for the current theme. They are stacked to remain readable on narrow screens and with larger text.

### Per-account destinations (2.0.6-plus.6)

Every account always shows one compact row: robot (generate on startup), keyboard, clipboard, and antenna (Jelling). Select one destination or none; tap the selected destination to clear it. An account with a destination sends each newly generated code there, both on tap and on startup. The Share button reuses that destination; with none selected, it follows the enabled global sharing methods. Global transport switches and keyboard destination management remain in Advanced settings; the global startup/auto-share switches are removed.

Manual accounts may share destinations. Among startup-enabled accounts, each destination belongs to one account. Enabling startup generation or changing the destination of a startup-enabled account keeps the latest configuration and disables startup generation on conflicting accounts, preserving their destinations and showing an explanatory toast. Multiple accounts with no destination may generate at startup. Checkbox changes never generate a code or advance HOTP.

On upgrade, previously enabled startup selections are preserved; selections hidden behind a disabled legacy global switch remain disabled. The old chooser-based automatic sharing has no explicit destination and is cleared so the user can choose one in the new row. Clipboard startup delivery runs first; keyboard and Jelling foreground sessions are queued, with expired queued codes discarded without regeneration. Physical testing remains required.
