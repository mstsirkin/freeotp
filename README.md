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

The share panel offers Clipboard, Jelling, and **Send as keyboard**. **Sharing settings** in the burger menu enables or disables Jelling and Bluetooth keyboard independently; clipboard is always available. Both Bluetooth methods are enabled by default, with keyboard hidden on Android versions before 9. Disabled methods are not constructed and do not request their transport permissions.

**Manage keyboard destinations** opens the device screen reused from TXT to BT: select a remembered destination, add a device by finding a visible computer or making the phone visible, and filter known accessories. Show all devices and diagnostics are in the overflow menu. Keyboard sharing launches this private foreground screen with the displayed code and sends once to the selected device; without a destination, select or pair one first. It does not regenerate a code or advance HOTP. The keyboard classes are built into FreeOTP Plus; no companion Android app is needed.

Keep the keyboard screen visible during sending. Numbers are normally unaffected by letter-language changes, while letters and symbols require US English with Caps Lock off. Linux must authorize keyboard access and trust the phone for incoming reconnections. Failures preserve the code for manual retry; partial sends are not retried automatically.

The APK also includes the earlier HOTP backup-counter fix (separate commit). Physical phone/computer testing remains required for this integration. The standalone keyboard regression checks can be compiled with KeyboardCodec.java and KeyboardReports.java and run as KeyboardCodecTest / KeyboardReportsTest.
