# Shizuku

An unofficial fork of the original [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku), focused on compatibility fixes for Android 17 while retaining the original app's core functionality.

## Current Version

| Item | Status |
| --- | --- |
| Upstream base | Original RikkaApps [v13.6.0](https://github.com/RikkaApps/Shizuku/releases/tag/v13.6.0), commit `2650830c` |
| Current build | `13.6.0.r1369-razgame-rikka-beta` |
| App name | Shizuku |
| Compatibility fixes | Android 17 / API 37 package enumeration and protected ADB settings writes |
| Compile / target SDK | 36, matching the upstream base |
| Minimum Android version | Android 7.0 / API 24 |

This is a community-maintained build, not an official RikkaApps release. The patches address the Android 17 changes described below; support for every device and future beta is not implied.

## Android 17 Fixes

- **Installed applications:** Android 17 changes `IPackageManager.getInstalledPackages(...)` from `ParceledListSlice<PackageInfo>` to `PackageInfoList`. [InstalledPackagesCompat](server/src/main/java/rikka/shizuku/server/util/InstalledPackagesCompat.java) uses the device's framework proxy to read the new result. The manager routes these calls through `ShizukuBinderWrapper`; older Android versions retain the original `PackageManagerApis` path, including its `int` / `long` flags compatibility.
- **Startup with restricted debugging settings:** recent Android 17 builds require the system-only `MANAGE_DEBUGGING` permission for some ADB settings writes, even when `WRITE_SECURE_SETTINGS` is granted. [AdbSettings](manager/src/main/java/moe/shizuku/manager/adb/AdbSettings.kt) handles a denied write so manual and boot startup can continue using an already enabled debugging service. Debugging may need to be enabled manually in Developer options.
- **Boot discovery:** on Android 17, discovery is attempted even when a debugging setting reports a disabled value to the app.

The app retains upstream's target SDK 36 and the same six declared permissions. No new local-network permission is added to this build. Updating the target SDK to 37 would require separate permission handling and validation.

## Original Project Base

The active `rikka-android17` branch is based directly on RikkaApps/Shizuku. It does not include the additional features from thedjchi's fork:

- Self-repackaging / stealth mode and in-app APK installation or uninstallation.
- TV pairing AccessibilityService.
- Watchdog and the fork's extended background-start machinery.

Rikka's original startup flow, basic boot receiver, wireless pairing, and Shizuku API remain. This project previously published thedjchi-based builds; the old `android17-razgame-beta` branch and releases through r1367 belong to that earlier line. The GitHub fork relationship still records that history, but it does not describe the code base of the current branch.

## Downloads And Validation

[Releases](https://github.com/RazGame/Shizuku/releases) contains the previously published builds. The current Rikka-based r1369 build has been built locally and has not yet been published as a GitHub release.

Debug and release APKs build successfully. The release signature and manifest were checked; the permission set matches the official Rikka v13.6.0 APK, and the added fork dependencies are absent. The owner reports that the minimal r1368 build works; r1369 only restores the visible app name to Shizuku. The project currently has no manager unit-test sources.

These checks do not establish a Google Play Protect classification or guarantee that a warning will disappear. Report issues with the exact APK version, Android build, and relevant logs in [this repository](https://github.com/RazGame/Shizuku/issues).

The local APKs use the existing development signing certificate, not Rikka's certificate. They can update compatible RazGame builds but cannot be installed as an update over an official APK signed by Rikka. No production signing-key migration has been performed.

## Background

When developing apps that requires root, the most common method is to run some commands in the su shell. For example, there is an app that uses the `pm enable/disable` command to enable/disable components.

This method has very big disadvantages:

1. **Extremely slow** (Multiple process creation)
2. Needs to process texts (**Super unreliable**)
3. The possibility is limited to available commands
4. Even if ADB has sufficient permissions, the app requires root privileges to run

Shizuku uses a completely different way. See detailed description below.

## Upstream User Guide

The original project's user guide is available at <https://shizuku.rikka.app/>. Downloads from that site are official upstream builds, not builds from this fork.

## How does Shizuku work?

First, we need to talk about how app use system APIs. For example, if the app wants to get installed apps, we all know we should use `PackageManager#getInstalledPackages()`. This is actually an interprocess communication (IPC) process of the app process and system server process, just the Android framework did the inner works for us.

Android uses `binder` to do this type of IPC. `Binder` allows the server-side to learn the uid and pid of the client-side, so that the system server can check if the app has the permission to do the operation.

Usually, if there is a "manager" (e.g., `PackageManager`) for apps to use, there should be a "service" (e.g., `PackageManagerService`) in the system server process. We can simply think if the app holds the `binder` of the "service", it can communicate with the "service". The app process will receive binders of system services on start.

Shizuku guides users to run a process, Shizuku server, with root or ADB first. When the app starts, the `binder` to Shizuku server will also be sent to the app.

The most important feature Shizuku provides is something like be a middle man to receive requests from the app, sent them to the system server, and send back the results. You can see the `transactRemote` method in `rikka.shizuku.server.ShizukuService` class, and `moe.shizuku.api.ShizukuBinderWrapper` class for the detail.

So, we reached our goal, to use system APIs with higher permission. And to the app, it is almost identical to the use of system APIs directly.

## Developer guide

### API & sample

https://github.com/RikkaApps/Shizuku-API

### Migrating from pre-v11

> Existing applications still works, of course.

https://github.com/RikkaApps/Shizuku-API#migration-guide-for-existing-applications-use-shizuku-pre-v11

### Attention

1. ADB permissions are limited

   ADB has limited permissions and different on various system versions. You can see permissions granted to ADB [here](https://github.com/aosp-mirror/platform_frameworks_base/blob/master/packages/Shell/AndroidManifest.xml).

   Before calling the API, you can use `ShizukuService#getUid` to check if Shizuku is running user ADB, or use `ShizukuService#checkPermission` to check if the server has sufficient permissions.

2. Hidden API limitation from Android 9

   As of Android 9, the usage of the hidden APIs is limited for normal apps. Please use other methods (such as <https://github.com/LSPosed/AndroidHiddenApiBypass>).

3. Android 8.0 & ADB

   At present, the way Shizuku service gets the app process is to combine `IActivityManager#registerProcessObserver` and `IActivityManager#registerUidObserver` (26+) to ensure that the app process will be sent when the app starts. However, on API 26, ADB lacks permissions to use `registerUidObserver`, so if you need to use Shizuku in a process that might not be started by an Activity, it is recommended to trigger the send binder by starting a transparent activity.

4. Direct use of `transactRemote` requires attention

   * The API may be different under different Android versions, please be sure to check it carefully. Also, the `android.app.IActivityManager` has the aidl form in API 26 and later, and `android.app.IActivityManager$Stub` exists only on API 26.

   * `SystemServiceHelper.getTransactionCode` may not get the correct transaction code, such as `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages` does not exist on API 25 and there is `android.content.pm.IPackageManager$Stub.TRANSACTION_getInstalledPackages_47` (this situation has been dealt with, but it is not excluded that there may be other circumstances). This problem is not encountered with the `ShizukuBinderWrapper` method.

## Developing Shizuku itself

### Build

Use JDK 21, Android SDK Platform 36, Build Tools 36.0.0, NDK 29.0.13113456, and CMake 3.31.6. Use the project's Gradle wrapper (8.14).

```shell
git clone --recurse-submodules https://github.com/RazGame/Shizuku.git
cd Shizuku
./gradlew :manager:assembleDebug :manager:assembleRelease -PversionCodeOverride=1369
```

On Windows, use `gradlew.bat`. Set `ANDROID_HOME` or configure `sdk.dir` in your untracked `local.properties`. Signing can be configured with an untracked `signing.properties`; without it, the project falls back to the local development key.

APKs are generated in `manager/build/outputs/apk/debug/` and `manager/build/outputs/apk/release/`. The explicit revision matches the current build; without `versionCodeOverride`, the version code is derived from the current branch's commit count.

See [build notes](TEST_BUILD.md) for the compatibility changes and validation limits. Keep upstream's license and notices; the original license section follows below.

The `:manager:assembleDebug` task generates a debuggable server. You can attach a debugger to `shizuku_server` to debug the server. Be aware that, in Android Studio, "Run/Debug configurations" - "Always install with package manager" should be checked, so that the server will use the latest code.

## License

The code for this project is available under the Apache-2.0 license.

### Exceptions

* You are **FORBIDDEN** to use image files listed below in any way (unless for displaying Shizuku itself).

  ```
  manager/src/main/res/mipmap-hdpi/ic_launcher.png
  manager/src/main/res/mipmap-hdpi/ic_launcher_background.png
  manager/src/main/res/mipmap-hdpi/ic_launcher_foreground.png
  manager/src/main/res/mipmap-xhdpi/ic_launcher.png
  manager/src/main/res/mipmap-xhdpi/ic_launcher_background.png
  manager/src/main/res/mipmap-xhdpi/ic_launcher_foreground.png
  manager/src/main/res/mipmap-xxhdpi/ic_launcher.png
  manager/src/main/res/mipmap-xxhdpi/ic_launcher_background.png
  manager/src/main/res/mipmap-xxhdpi/ic_launcher_foreground.png
  manager/src/main/res/mipmap-xxxhdpi/ic_launcher.png
  manager/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png
  manager/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png
  ```

* For the project as a whole, it is not free.
You are **FORBIDDEN** to distribute the apk compiled by **you**
(including modified, e.g., rename app name "Shizuku" to something else)
to any store (IBNLT Google Play Store, F-Droid, Amazon Appstore etc.).
