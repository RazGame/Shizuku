# Rikka-based Android 17 beta build

This is an unofficial local build based on RikkaApps/Shizuku v13.6.0,
commit 2650830c, with its original API submodule pinned to 510fc988.

It does not include thedjchi fork additions: self-repackaging, the TV
accessibility service, watchdog, or the fork's extended background startup.
Rikka's original startup behavior and basic boot receiver are retained.

Changes:

- Android 17 package enumeration uses the device's own IPackageManager proxy
  and reads PackageInfoList. The manager explicitly wraps Binder calls with
  ShizukuBinderWrapper; older Android versions keep PackageManagerApis.
- Denied ADB settings writes no longer crash manual or boot startup.
- SDK and target SDK remain 36, as in the reference Rikka release. No extra
  manifest permissions are added. Android 17 local-network permissions are
  required for target 37+ according to Android's developer documentation.
- The visible app label is Shizuku; the version retains the unofficial beta suffix.

Signing retains the existing local development certificate for update compatibility.
The certificate is not Rikka's. This is not a production signing migration.
The application ID is retained for compatibility with existing Shizuku clients.
Installing this APK replaces an installed build with the same application ID
and compatible signature. It cannot update an APK signed with Rikka's key.

Build:

```powershell
.\gradlew.bat :manager:assembleDebug :manager:assembleRelease -PversionCodeOverride=1369
```

Play Protect has not been tested for this artifact. No guarantee is made that
it will remove a warning or change Google's existing classification.
No APK has been uploaded to an external scanner, published, or installed as
part of producing this test build.

Local validation:

- Debug and release builds succeeded; the unit-test task reported NO-SOURCE.
- The decoded release APK manifest has exactly the same six uses-permission
  entries as the official v13.6.0 APK and only AdbPairingService.
- The release dependency graph contains no ARSCLib, apksig, WorkManager, or
  OkHttp dependency from thedjchi's additional features.
- Release signature verification succeeded with the existing local development
  certificate. The APK is not debuggable.
- The user reports that the r1368 build works well. The specific device and
  Play Protect scan result were not separately confirmed for that report.
- Revision r1369 restores the visible app label to Shizuku; runtime logic is unchanged.

References:

- https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0
- https://developer.android.com/privacy-and-security/local-network-permission
- https://developers.google.com/android/play-protect/warning-dev-guidance
