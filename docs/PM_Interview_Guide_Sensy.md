# Sensy - Product Manager Interview Breakdown

This document provides a comprehensive analysis of the **Sensy: Missed Call Manager** project, specifically tailored for a Product Manager (PM) interview. It breaks down the product strategy, technical architecture, crucial trade-offs, and future roadmap.

---

## 1. Product Overview & Value Proposition
- **Elevator Pitch:** Sensy is an Android-based personal auto-responder that protects a user's focus time (e.g., sleeping, gym, deep work) by automatically intercepting missed calls and sending contextual SMS replies to callers, including an estimated time of return.
- **Target Audience:** Busy professionals, athletes, students, and anyone who needs uninterrupted focus time but wants to maintain good communication etiquette and avoid "ghosting" their contacts.
- **Core Problem Solved:** It eliminates the anxiety of missing important calls and the perceived rudeness of ignoring people, perfectly balancing the need for deep focus with interpersonal communication.

## 2. Key Features & User Experience (UX)
- **Dynamic Presets:** Users can create custom statuses (e.g., "Gym" -> "working out"). This reduces the friction to activate the app to just a couple of taps.
- **Auto-Expiry (Time-boxing):** Users set a duration (e.g., 30 mins). The status auto-clears, preventing the classic problem of forgetting to turn off "Do Not Disturb" mode.
- **1-Tap Widget:** A home screen widget allows for activation/deactivation without opening the app, minimizing context switching.
- **Recent Statuses:** The app remembers recent custom durations and text, making repeat use seamless.

## 3. High-Level Technical Architecture (For PMs)
*PMs don't need to code, but they must understand how the system works to manage engineering timelines and communicate technical constraints.*
- **UI Layer:** Built with **Jetpack Compose**, Android's modern declarative UI framework.
- **Event Interception (`BroadcastReceivers`):** Uses a `CallReceiver` listening to `PHONE_STATE` changes to detect when a call transitions from `RINGING` to `IDLE` (indicating a missed call).
- **Background Execution (Services):**
    - `StatusService`: A **Foreground Service** that keeps the app alive even when closed. It displays a persistent notification (an Android OS requirement for foreground services).
    - `SmsDispatcherService`: Handles the actual sending of the SMS asynchronously to prevent blocking the main thread.
- **Scheduling (`Alarms`):** Uses Android's `AlarmManager` with an `AlarmReceiver` to accurately clear the status exactly when the timer expires.

## 4. Crucial PM Talking Points (Trade-offs & Challenges)
*Interviewers love when candidates discuss trade-offs, edge cases, and platform constraints.*

### A. The "Battery vs. Reliability" Trade-off (Crucial Android Concept)
- **The Problem:** The Android OS aggressively kills background apps to save battery (via Doze Mode / App Standby).
- **The Workaround:** Sensy requires a Foreground Service (persistent notification) and asks users to explicitly "Disable Battery Optimization" in Android settings.
- **PM Impact:** This introduces friction during onboarding. As a PM, you must weigh the drop-off rate during onboarding against the churn rate of users whose auto-responder failed to trigger because the app was killed in the background.

### B. Privacy, Trust, and Play Store Policies
- **Sensitive Permissions:** Sensy requests `READ_PHONE_STATE`, `READ_CALL_LOG`, and `SEND_SMS`.
- **Play Store Risk:** Google Play has extremely strict policies around SMS and Call Log access. Getting this app approved requires submitting a declaration form proving it is a core feature (which it is, as an auto-responder).
- **User Trust:** Users are granting immense power to the app. Onboarding must clearly and transparently explain *why* these permissions are needed to build trust.

### C. Edge Cases & Spam Prevention (Future Considerations)
- **Spamming Callers:** If someone calls 5 times in 5 minutes, does Sensy send 5 SMS messages? 
  - *PM Solution:* Propose a rate limiter or cooldown per contact in the `SmsDispatcherService`.
- **SMS Costs:** SMS isn't free on all carrier plans globally. 
  - *PM Solution:* Add a warning about potential carrier charges during onboarding.
- **Spam Calls/Robocalls:** Do we auto-reply to telemarketers? 
  - *PM Solution:* Add a setting to "Only reply to numbers in My Contacts."

## 5. Future Roadmap & Scaling (Vision)
*Show the interviewer you can think beyond the MVP.*
- **WhatsApp Integration:** Fallback to WhatsApp auto-replies via Accessibility Services if SMS is not preferred.
- **Smart Triggers (Automation):** Auto-activate the "Driving" status via Bluetooth connection to a car, or "Gym" status based on Geofencing/GPS location.
- **VIP Lists:** Always let calls ring through and bypass the auto-responder for specific contacts (e.g., spouse, parents).
- **Calendar Integration:** Automatically sync statuses based on Google Calendar meetings.
