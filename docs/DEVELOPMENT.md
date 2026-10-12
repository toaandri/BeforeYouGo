# Development

BeforeYouGo is a native Android app built with Kotlin, Jetpack Compose and Material 3. The public repository contains source code; publishing it makes that code and its committed history visible.

## Requirements

- Android SDK 37; minimum supported Android version: 8.0 / API 26.
- JDK 25 for the Gradle daemon, as specified by the project.
- The included Gradle 9.6.0 wrapper.

Configure your local SDK through Android Studio or an untracked `local.properties` file.

## Build and verify

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest lintDebug
.\gradlew.bat assembleDebugAndroidTest
```

On macOS and Linux, use `./gradlew`. Building the instrumented test APK does not execute its tests. Run connected tests only on a dedicated emulator or test device, where reinstalling or clearing app data is safe.

**Verified on 12 October 2026:** debug and instrumented test APKs build, 10 unit tests and 13 instrumented tests pass on an isolated Android 15 / API 35 emulator. Lint reports zero errors and 33 non-blocking warnings. This does not replace physical-device release testing.

English and French are included in every App Bundle, so switching languages works offline. The project is not configured with production signing credentials.

## Keep private material local

The `.gitignore` excludes signing keys, common credential files, local environment settings, IDE configuration and `.private/`. Internal notes and unpublished captures are kept locally; they are not required to build the app.

Before publishing, inspect the staged files and scan both changes and history with a tool such as [Gitleaks](https://github.com/gitleaks/gitleaks):

```sh
gitleaks git --staged --redact .
gitleaks git --log-opts="--all" --redact .
```

These scans detect known secret patterns and do not guarantee that every sensitive value will be found. Do not store production passwords in build scripts or source code. Keep upload keys outside the repository and supply signing credentials through local or protected CI configuration.

If a credential was committed, revoke or rotate it first. Deleting its current file or adding an ignore rule does not remove earlier copies. Follow [GitHub's sensitive-data removal guidance](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/removing-sensitive-data-from-a-repository) and [Android's signing guidance](https://developer.android.com/studio/publish/app-signing).
