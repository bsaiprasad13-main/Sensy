# Sensy: Missed Call Manager

Sensy is an Android application that acts as your personal auto-responder. When you are deep in work, working out, or simply taking a nap, Sensy protects your focus time while keeping your callers informed.

## How It Works

Sensy runs reliably in the background on your Android device. When you activate a status (e.g., "Sleeping" for 30 minutes), Sensy will automatically intercept any incoming missed calls and reply with a personalized SMS message to the caller. 

For example, if you miss a call while your status is set to "Gym", the caller will automatically receive:
> *"Hi, this is Sensy, [Your Name]'s assistant. Right now he is working out. He will be back in 30 mins. Thank you."*

## Features
- **Dynamic Key-Value Presets:** Define custom button labels and status messages to fit your lifestyle.
- **Home Screen Widget:** 1-tap activation right from your launcher without needing to open the app.
- **Background Persistence:** Utilizes Android Foreground Services to ensure the app stays alive and responds to calls even when your phone is locked.
- **Auto-Expiry:** Statuses automatically expire after the duration you set.
- **Android 14+ Compatibility:** Fully supports modern Android constraints like Foreground Service Special Use and Exact Alarms.

## How to Use on Your Device

To install and run Sensy on your own Android device:
1. Clone this repository to your local machine.
2. Open the project in **Android Studio**.
3. Connect your Android device via USB (ensure USB Debugging is enabled in Developer Options).
4. Click the Green **Play** button in Android Studio to build and install the APK on your device.
5. On first launch, the app will request necessary permissions (Call Log, Phone State, SMS, and Notifications). Please grant these for the app to function correctly.
6. **Important:** Android heavily optimizes background apps. You must disable Battery Optimization for Sensy in your device settings for the service to run reliably.

## Built With AI

This entire project was developed and refined using the **Antigravity AI IDE**. Leveraging an AI pair programmer made the process incredibly smooth—from scaffolding the initial Android Foreground Services, to navigating complex Android `RemoteViews` widget constraints, right down to perfecting the UI with Jetpack Compose. If you are building Android apps, using an AI-native IDE like Antigravity accelerates your workflow exponentially!
