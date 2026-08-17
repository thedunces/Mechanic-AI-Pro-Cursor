# Production Checklist — Mechanic AI Pro

This checklist covers the remaining console and signing steps needed to publish on Google Play. App-side production hardening for legal links, account deletion, Google Sign-In, Play RTDN, R8, and disclaimers is already in the codebase.

## Google Play subscriptions

- [ ] Create subscription product `mechanic_ai_pro_monthly` in Play Console.
- [ ] Add and activate base plan `monthly`; set its price and auto-renewing billing period.
- [ ] Upload an internal-testing AAB before testing billing; install the app from the Play test link.
- [ ] Add license tester accounts and accept the tester opt-in link.
- [ ] Link the Google Cloud project to the Play Console developer account.
- [ ] Grant the Cloud Functions runtime service account access to the Google Play Android Developer API.
- [ ] Enable `androidpublisher.googleapis.com` in Google Cloud.
- [ ] Create a Pub/Sub topic named `play-rtdn` and point Play Console Real-time Developer Notifications at it.
- [ ] Deploy functions so `playRtdn` can revoke or refresh entitlements on refund, expiration, and renewal.
- [ ] Verify upgrade, renewal, grace period, cancellation, expiration, refund, restore, and pending-purchase behavior.

## Firebase Configuration

- [ ] Replace the placeholder `app/google-services.json` with the real file from your Firebase project (copy from `app/google-services.json.example` only as a reminder of the shape).
- [ ] Confirm `.firebaserc` uses your real Firebase project ID (`mechanic-ai-pro-final` today).
- [ ] Enable **Anonymous**, **Email/Password**, and **Google** sign-in in Firebase Auth.
- [ ] Add the app's SHA-1 and SHA-256 fingerprints (debug and release) to the Firebase Android app.
- [ ] Create a Cloud Firestore database and deploy rules and indexes:
  ```bash
  firebase deploy --only firestore
  ```
- [ ] Enable `firestore.googleapis.com` if the console still reports it disabled.
- [ ] Set the Gemini key as a Firebase secret and deploy functions:
  ```bash
  cd functions
  npm install
  firebase functions:secrets:set GOOGLE_API_KEY
  firebase deploy --only functions
  ```
- [ ] Enable the [Generative Language API](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com) for the API key.
- [ ] Deploy hosted privacy and terms pages:
  ```bash
  firebase deploy --only hosting
  ```
  Confirm `https://mechanic-ai-pro-final.web.app/privacy.html` and `/terms.html` load. If the project ID changes, update `app/src/main/res/values/strings.xml`.
- [ ] Register the app in **Google Play Console** and enable Play Integrity API.
- [ ] Add an App Check debug token for local development (see Firebase console logs on first debug run).
- [ ] In Firebase App Check, enforce Play Integrity for Auth, Firestore, and Functions in production.

## Android App

- [ ] Copy `keystore.properties.example` to `keystore.properties` and create a release keystore:
  ```bash
  keytool -genkeypair -v -keystore release.keystore -alias mechanic-ai-pro -keyalg RSA -keysize 2048 -validity 10000
  ```
  Keep the keystore and passwords offline. Losing them prevents Play Store updates.
- [ ] Increment `versionCode` in `app/build.gradle.kts` for every Play upload. `versionName` is `1.0.0`.
- [ ] Build a signed release bundle: Android Studio **Build > Generate Signed App Bundle**, or `./gradlew bundleRelease` after the Gradle wrapper exists.
- [ ] Smoke-test the minified release build (R8 is on).
- [ ] Replace the adaptive launcher icon with final brand artwork if you have a designer asset. A production wrench icon is already in the repo.
- [ ] Add Play Store feature graphic (1024x500) and phone screenshots.
- [ ] Test on a range of devices and Android versions, especially Android 12+ Bluetooth permissions.

## Legal & Compliance

- [ ] Confirm the hosted privacy policy and terms URLs open from Settings.
- [ ] Confirm the AI/OBD safety disclaimer appears on diagnosis screens.
- [ ] Confirm account deletion from Settings removes Auth and Firestore data.
- [ ] Complete the Play Console content rating questionnaire and Data safety form.

## Google Play Store

- [ ] Create the Play Store listing with screenshots, short/long description, and categorization.
- [ ] Set content rating via the Play Console questionnaire.
- [ ] Configure pricing and distribution countries.
- [ ] Upload the signed AAB to internal testing, then production.
