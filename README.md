# Mechanic AI Pro

An Android diagnostic app for DIY automotive mechanics, written in Kotlin with Jetpack Compose and backed by Firebase.

## Features Implemented

- **Anonymous authentication** with Firebase Auth, upgradeable to Google Sign-In.
- **Modern Jetpack Compose UI** with Material 3, type-safe navigation, and a dashboard.
- **Vehicle management** with VIN decoding via the free NHTSA vPIC API and Firestore persistence.
- **Manual diagnosis** with OBD-II codes, live data parameters, symptoms, and notes.
- **AI-powered diagnosis** via a Firebase Cloud Function calling Gemini with structured JSON output.
- **Bluetooth OBD-II scanning** for ELM327 adapters: read/clear DTCs and read live data.
- **Diagnosis history** stored per user in Firestore.
- **Firebase Security Rules** and **App Check** (debug for development, Play Integrity for production).
- **Production checklist** and **privacy policy** drafts for Google Play Store readiness.

## Tech Stack

- Kotlin + Jetpack Compose + Material 3
- MVVM + Repository pattern + Hilt DI
- Firebase: Auth, Firestore, Cloud Functions, App Check, Analytics, Crashlytics
- NHTSA vPIC API for vehicle validation
- Google Gemini API via Cloud Functions for AI diagnosis
- Bluetooth Classic SPP for ELM327 OBD-II adapters

## Project Structure

```
app/                  Android application
├── src/main/java/.../com/mechanicai/pro/
│   ├── data/         Repositories, remote APIs, local persistence
│   ├── di/           Hilt dependency injection
│   ├── domain/       Use cases and domain models
│   └── presentation/ Screens, ViewModels, navigation, theme
functions/            Firebase Cloud Functions (TypeScript)
firestore.rules       Firestore security rules
firestore.indexes.json Firestore indexes
PRIVACY_POLICY.md     Draft privacy policy
PRODUCTION_CHECKLIST.md  Pre-launch checklist
```

## Getting Started

1. Install Android Studio Ladybug or newer.
2. Open this folder in Android Studio.
3. Create a Firebase project at [https://console.firebase.google.com](https://console.firebase.google.com).
4. Add an Android app with package name `com.mechanicai.pro` and download `google-services.json` into `app/`.
5. Enable **Anonymous** authentication in Firebase Auth. Optionally enable **Google** sign-in.
6. Create a Cloud Firestore database and deploy `firestore.rules` and `firestore.indexes.json`.
7. In `functions/`, the `GOOGLE_API_KEY` is already configured in `.env` for development. For production, set it as a secret or environment variable via Firebase:
   ```bash
   cd functions
   npm install
   firebase deploy --only functions
   ```
   If you deploy to production, use `firebase functions:secrets:set GOOGLE_API_KEY` or `firebase functions:env:set GOOGLE_API_KEY=YOUR_KEY`.
   Make sure the [Generative Language API](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com) is enabled for the API key in Google Cloud Console.
8. Register the app in Google Play Console and enable Play Integrity API for App Check in production.
9. Sync Gradle and run the app.

### Note on Gradle Wrapper

The `gradle-wrapper.jar` is intentionally not included. After opening the project in Android Studio, the IDE will generate it automatically when you sync Gradle. Alternatively, run:
```bash
gradle wrapper
```
if you have a local Gradle installation.

## Phase Status

| Phase | Status | Description |
|-------|--------|-------------|
| 1 | Completed | Project skeleton, Firebase integration, anonymous auth, navigation, dashboard |
| 2 | Completed | Vehicle management, NHTSA vPIC integration, Firestore CRUD, security rules |
| 3 | Completed | Manual diagnosis flow, Cloud Function AI endpoint, diagnosis result display, history |
| 4 | Completed | Bluetooth OBD-II ELM327 scanning, read/clear codes, live data, AI diagnosis from scan |
| 5 | Completed | Production hardening stubs, privacy policy, checklist, RAG ingestion stub, App Check config |

## Important Notes

- **Repair manual data**: The plan includes a future RAG layer. Do not scrape copyrighted manuals without verifying their license or terms of service.
- **Safety**: AI-generated diagnostic advice is not a substitute for a professional mechanic. Always follow safety warnings and consult a professional for critical issues.
- **Bluetooth permissions**: On Android 12+, the app requests `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime. Pair the ELM327 adapter in Android settings before scanning.

## License

TBD
