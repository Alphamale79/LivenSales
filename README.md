# Liven Sales — Android app

A standalone, offline-first Android sales-order app for Liven Pharma. The app has two roles:

- **Sales user:** creates quantity-only orders, sees administrator-set prices, saves order PDFs, and shares them using Android's share sheet.
- **Administrator:** unlocks the local admin panel with a PIN to add/edit/deactivate products, update rates, manage distributor and salesperson lists, and change the PIN.

The supplied 42-product master and a vector recreation of the supplied logo are included. Orders and settings are stored on the device; there is no server or cloud sync.

## Open and build

Open this repository in Android Studio (Giraffe or newer), install Android SDK Platform 35 if prompted, allow Gradle sync, and choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The project uses Android Gradle Plugin 8.7.3 and Gradle 8.9. If Android Studio asks to update the Gradle wrapper, accept the compatible wrapper update.

## First launch

The initial administrator PIN is **1234**. Open **Admin** and change it before deployment. Sales users can create orders without entering the admin PIN. The administrator PIN is a convenience gate stored locally, not strong device authentication; use Android device security and a signed release build for deployment.

PDF sharing opens Android's chooser so the user confirms the target app and recipient. Direct/background WhatsApp sending is intentionally not implemented. Data is local to each device. Validate PDF output and product rates before operational use.
