# Mizan Android CI

The repository contains a GitHub Actions workflow at `.github/workflows/android-build.yml`. The workflow builds the Android application from the exact commit checked out by GitHub Actions.

## Debug APK

A push to `main`, a pull request targeting `main`, or a manual workflow dispatch installs the Android SDK packages required by the current project, creates the project-required debug keystore on the ephemeral runner, and runs:

```bash
./gradlew assembleDebug
```

The generated APK is verified and uploaded as an Actions artifact named `mizan-debug-apk-<commit-sha>`.

The current project contains its Supabase URL, Supabase anonymous client key, and Google web client ID in the Gradle build configuration. These are client-side configuration values used by the app. The workflow does not add any new application secret and does not commit `.env`, a keystore, or passwords.

## Release APK and AAB

Release output is attempted only when all of the following GitHub Actions secrets exist:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_PASSWORD`

The workflow then runs `assembleRelease bundleRelease` and uploads separate release APK and AAB artifacts. The release keystore is decoded only on the ephemeral runner and is never committed. If the secrets are absent, Debug still builds and the workflow reports that Release signing was skipped.

## Local build

Use the committed Gradle Wrapper with JDK 17. The project archive does not contain a release keystore. Do not commit `.env`, `debug.keystore`, `my-upload-key.jks`, or password values.
