# Mechanic AI Pro

An Android diagnostic app for DIY automotive mechanics, written in Kotlin with Jetpack Compose and backed by Firebase.

## Features Implemented

- **Guest, email, and Google authentication** with Firebase Auth. Guest accounts can be linked so data and subscriptions restore.
- **Modern Jetpack Compose UI** with Material 3, type-safe navigation, and a dashboard.
- **Vehicle management** with VIN decoding via the free NHTSA vPIC API and Firestore persistence.
- **Manual diagnosis** with OBD-II codes, live data parameters, symptoms, and notes.
- **AI-powered diagnosis** via a Firebase Cloud Function calling Gemini with structured JSON output.
- **Bluetooth OBD-II scanning** for ELM327 adapters: read/clear DTCs and read live data.
- **Diagnosis history** stored per user in Firestore.
- **Firebase Security Rules** and **App Check** (debug for development, Play Integrity for production).
- **Production hardening**: Play Integrity App Check, R8, account deletion, hosted privacy/terms, in-app review, and Play Real-time Developer Notifications.

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
public/               Hosted privacy policy and terms
PRIVACY_POLICY.md     Privacy policy source
TERMS_OF_SERVICE.md   Terms of service source
PRODUCTION_CHECKLIST.md  Pre-launch checklist
```

## Getting Started

1. Install Android Studio Ladybug or newer.
2. Open this folder in Android Studio.
3. Create a Firebase project at [https://console.firebase.google.com](https://console.firebase.google.com).
4. Add an Android app with package name `com.mechanicai.pro` and download `google-services.json` into `app/`.
5. Enable **Anonymous**, **Email/Password**, and **Google** authentication. Add SHA-1/SHA-256 fingerprints for debug and release keystores.
6. Create a Cloud Firestore database and deploy rules, indexes, functions, and hosting:
   ```bash
   cd functions
   npm install
   firebase functions:secrets:set GOOGLE_API_KEY
   firebase deploy
   ```
   Enable the [Generative Language API](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com) for the API key.
7. Create `keystore.properties` from `keystore.properties.example` and a release keystore. See `PRODUCTION_CHECKLIST.md`.
8. Register the app in Google Play Console, create subscription `mechanic_ai_pro_monthly` / base plan `monthly`, enable Play Integrity, and point Real-time Developer Notifications at the `play-rtdn` Pub/Sub topic.
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
| 5 | Completed | Production hardening: legal pages, account deletion, Google Sign-In, RTDN, R8, Play review |

## Important Notes

- **Repair manual data**: The plan includes a future RAG layer. Do not scrape copyrighted manuals without verifying their license or terms of service.
- **Safety**: AI-generated diagnostic advice is not a substitute for a professional mechanic. Always follow safety warnings and consult a professional for critical issues.
- **Bluetooth permissions**: On Android 12+, the app requests `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` at runtime. Pair the ELM327 adapter in Android settings before scanning.

## License

TBD
