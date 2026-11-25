# WalX by Nor Gaxmer Builds

Personal finance and money-tracking Android app built with Kotlin, Jetpack Compose, Firebase Authentication (Google Sign-In), and Firebase AI Logic.

## How to Build & Download the APK from GitHub

1. **(One-Time for Google Sign-In & AI Copilot)** In GitHub, open **Settings > Secrets and variables > Actions > New repository secret**, name it `GOOGLE_SERVICES_JSON`, and paste the contents of your `app/google-services.json` file.
2. Open the **Actions** tab at the top of this GitHub repository.
3. Click **Build & Release WalX APK** on the left sidebar.
4. Click **Run workflow** (optionally enter a version label like `v1.0.0`, or leave blank to auto-number) and click the green **Run workflow** button.
5. Wait ~2–3 minutes until the run turns green, then open the **Releases** page on this repository and tap **`WalX-debug.apk`** to download and install directly on your Android phone.
