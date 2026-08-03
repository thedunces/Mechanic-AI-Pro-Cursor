# Production Checklist — Mechanic AI Pro

This checklist covers the steps needed to make the app ready for the Google Play Store.

## Google Play subscriptions

- [ ] Create subscription product `mechanic_ai_pro_monthly` in Play Console.
- [ ] Add and activate base plan `monthly`; set its price and auto-renewing billing period.
- [ ] Upload an internal-testing AAB before testing billing; install the app from the Play test link.
- [ ] Add license tester accounts and accept the tester opt-in link.
- [ ] Link the Google Cloud project to the Play Console developer account.
- [ ] Grant the Cloud Functions runtime service account access to the Google Play Android Developer API.
- [ ] Enable `androidpublisher.googleapis.com` in Google Cloud.
- [ ] Configure Real-time Developer Notifications before public launch so refunds and revocations remove access promptly.
- [ ] Verify upgrade, renewal, grace period, cancellation, expiration, refund, restore, and pending-purchase behavior.

## Firebase Configuration

- [ ] Replace the placeholder `app/google-services.json` with the real file from your Firebase project.
- [ ] Update `.firebaserc` with your real Firebase project ID.
- [ ] Enable **Anonymous** authentication in Firebase Auth.
- [ ] Enable **Google** sign-in provider in Firebase Auth and add SHA-1/SHA-256 fingerprints.
- [ ] Create a Cloud Firestore database and deploy `firestore.rules` and `firestore.indexes.json`.
- [ ] Enable `firestore.googleapis.com`; the project currently reports that the Firestore API is disabled.
- [ ] Deploy Cloud Functions (`functions/`). The dev key is already in `functions/.env`:
  ```bash
  cd functions
  npm install
  firebase deploy --only functions
  ```
- [ ] For production, move the API key out of `.env` and into a Firebase secret or environment variable:
  ```bash
  firebase functions:secrets:set GOOGLE_API_KEY
  # or
  firebase functions:env:set GOOGLE_API_KEY=YOUR_KEY
  ```
- [ ] Enable the [Generative Language API](https://console.cloud.google.com/apis/library/generativelanguage.googleapis.com) for the API key.
- [ ] Register the app in **Google Play Console** and enable Play Integrity API.
- [ ] Add App Check debug token for local development (see Firebase console logs on first run).

## AI & Knowledge Base

- [ ] Decide between `gemini-1.5-flash` and `gemini-1.5-pro` based on quality/cost trade-offs.
- [ ] Add web grounding / search retrieval to the Cloud Function for current sources.
- [ ] Build a RAG pipeline that ingests only public-domain or licensed repair manuals.
- [ ] **Legal review**: confirm copyright status of any repair manuals before ingestion.

## Android App

- [ ] Update `versionCode` and `versionName` in `app/build.gradle.kts` for each release.
- [ ] Create a release keystore and configure `signingConfigs.release` in `app/build.gradle.kts`.
- [ ] Verify ProGuard/R8 rules keep model classes and Firebase classes.
- [ ] Add a real privacy policy URL to the Play Store listing and in-app Settings screen.
- [ ] Add an in-app review API prompt after a successful diagnosis.
- [ ] Add adaptive launcher icons and Play Store feature graphics.
- [ ] Test on a range of devices and Android versions, especially Android 12+ Bluetooth permissions.

## Legal & Compliance

- [ ] Publish the privacy policy at a public URL.
- [ ] Ensure terms of service are available if required by your jurisdiction.
- [ ] Confirm that OBD-II data collection and AI advice disclaimers are shown to users.

## Google Play Store

- [ ] Create Play Store listing with screenshots, short/long description, and categorization.
- [ ] Set content rating via the Play Console questionnaire.
- [ ] Configure pricing and distribution countries.
- [ ] Upload app bundle (AAB) and complete the release review.
