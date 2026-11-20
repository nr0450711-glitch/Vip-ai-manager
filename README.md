# JARVIS VIP — Native Android AI Phone Manager

Repository: `nr0450711-glitch/jarvis-android` (`main` branch)

## Key Capabilities
- **Google Gemini AI Core (Hindi + English)**: Powered by Firebase AI Logic (`gemini-3.8-flash`) and Firebase App Check with zero embedded API keys.
- **Bilingual Speech & TTS**: Native Android `SpeechRecognizer` (`hi-IN` / `en-IN`) + `TextToSpeech` voice responses + `"Hey JARVIS"` / `"हे जार्विस"` wake mode loop.
- **Authorized Screen Automation (`JarvisAccessibilityService`)**: Directional & hands-free auto-scrolling, global navigation (`Back`, `Home`, `Recents`, `Notifications`), voice typing injection, and authorized social Like/Comment assist.
- **Confirmed Actions**: WhatsApp composer & Phone calling with explicit user confirmation dialogs, Camera + Gemini Vision analysis, Torch control, YouTube & Web search, and Instagram/Facebook photo upload assistance via Android Photo Picker.
- **Customizable VIP Dashboard**: Add and remove custom tools stored locally in Room.
- **Emergency Stop & Privacy Dashboard**: Persistent top HUD `STOP` button halts AI, TTS, mic, torch, and Accessibility automation instantly, plus live permissions & audit log visibility.

## GitHub Actions CI/CD
Workflow file: `.github/workflows/android.yml`
- Automatically builds `app-debug.apk` on every push or PR to `main`.
- Uploads `jarvis-vip-debug-apk` (`app/build/outputs/apk/debug/app-debug.apk`) as a downloadable artifact.

## Production Release Signing Instructions
`app/build.gradle.kts` reads release signing credentials from environment variables:
1. Generate a production upload keystore (do **not** commit it to Git):
   ```bash
   keytool -genkey -v -keystore my-upload-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
   ```
2. Register your release SHA-1 and SHA-256 fingerprints and production **reCAPTCHA Enterprise** site key (`recaptcha_site_key` in `app/src/main/res/values/firebase_applet_config.xml`) in your Firebase project for Firebase App Check.
3. For GitHub Actions release builds (`workflow_dispatch`), add these repository secrets in `nr0450711-glitch/jarvis-android`:
   - `RELEASE_KEYSTORE_BASE64`: `base64 -w 0 my-upload-key.jks`
   - `STORE_PASSWORD`: your keystore password
   - `KEY_PASSWORD`: your key password (`upload` alias)
4. To build locally:
   ```bash
   KEYSTORE_PATH="$(pwd)/my-upload-key.jks" STORE_PASSWORD="..." KEY_PASSWORD="..." gradle assembleRelease
   ```
