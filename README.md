# ProxyReset

Android app that clears the system HTTP proxy during a genuine device boot. The app also lets the user inspect and change the proxy from its foreground interface.

- Android package: `org.eu.suisei.proxyreset`
- Gradle module: `proxyreset`
- Minimum Android version: Android 17 (API 37)
- Language and UI: Kotlin, Jetpack Compose

## Build

Install JDK 17 and Android SDK Platform 37, then run:

```powershell
.\gradlew.bat :proxyreset:assembleDebug
```

The debug APK is written to `proxyreset/build/outputs/apk/debug/`.

The app requires the system `WRITE_SECURE_SETTINGS` permission to change the global HTTP proxy. Grant it from an authorized ADB shell when needed:

```sh
adb shell pm grant org.eu.suisei.proxyreset android.permission.WRITE_SECURE_SETTINGS
```

Without that permission, proxy controls remain disabled. Boot-time behavior depends on Android delivering the boot-completed broadcast.

## License

See `proxyreset/LICENSE-MaterialIcons` and `proxyreset/NOTICE` for the icon attribution and license. Other source files are provided without an additional license grant.
