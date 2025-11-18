# DreamZ (Android, Jetpack Compose)

An Android app for dream journaling, Google Drive sync, and local/offline storage.

## Requirements
- Android Studio Ladybug+ (or latest stable)
- JDK 17
- Android SDK installed
- A `local.properties` file in the project root (see `local.properties.example`)

## Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/<your-username>/dreamz.git
   cd dreamz
   ```

## Play App Signing & Production Release

1. Generate an **upload key** (Play will generate the app signing key when you enroll in Play App Signing):
   ```bash
   keytool -genkeypair -v -keystore ~/keys/dreamz-upload.jks \
       -keyalg RSA -keysize 2048 -validity 10000 -alias dreamz-upload
   ```
2. Copy `local.properties.example` to `local.properties` and fill in:
   - `google.drive.*` secrets for Drive sync.
   - `uploadStoreFile`, `uploadStorePassword`, `uploadKeyAlias`, `uploadKeyPassword` for the upload key. Keep this file out of Git.
3. Build the release bundle (signed with the upload key) so it can be uploaded to the Play Console:
   ```bash
   ./gradlew clean bundleRelease
   ```
   The signed bundle is generated at `app/build/outputs/bundle/release/app-release.aab`.
4. Copy the bundle into the `release/` directory (tracked in Git) when you need to share a specific build artifact, for example:
   ```bash
   cp app/build/outputs/bundle/release/app-release.aab release/DreamZ-v1.0.aab
   ```
5. Upload the generated AAB to the Play Console and complete the Play App Signing enrollment / release process.
