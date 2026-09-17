# Sensy: Automated Missed Call Status Assistant - Phase-Wise Implementation Plan

This document outlines the step-by-step technical implementation of the Sensy application, structured to minimize risk by validating core logic and UI before integrating complex system APIs.

## Phase 1: Project Setup & Core Logic
**Goal:** Establish the foundation and ensure the core calculation logic is flawless before interacting with Android system APIs.

- [x] **Step 1.1: Project Initialization** 
    - [x] Create a new Android Studio project (Kotlin) with an empty Activity.
    - [x] Declare necessary permissions in `AndroidManifest.xml` (`READ_PHONE_STATE`, `READ_CALL_LOG`, `SEND_SMS`, `POST_NOTIFICATIONS`).
- [x] **Step 1.2: Core Logic Implementation (Time Calculator)** 
    - [x] Create a pure Kotlin class that calculates remaining time given a start time and duration.
    - [x] Write comprehensive unit tests for this class to handle edge cases (e.g., negative time, "no limit" scenarios, zero duration).
- [x] **Step 1.3: Data Layer Setup** 
    - [x] Implement a `StatusRepository` using `SharedPreferences`.
    - [x] Create methods to `saveStatus()`, `getActiveStatus()`, and `clearStatus()`. Data stored should include status text, start timestamp, and total duration minutes.

## Phase 2: UI & Local Storage Integration
**Goal:** Allow the user to interact with the app, set a status, and see it persist locally.

- [x] **Step 2.1: Basic UI Layout (`MainActivity`)** 
    - [x] Build the main screen with preset status buttons (Cricket, Driving, Bath).
    - [x] Add a custom text input field for ad-hoc statuses.
    - [x] Add a duration selector (Preset minutes, Custom minutes, No Limit).
- [x] **Step 2.2: Save & Clear Actions** 
    - [x] Wire the UI to the `StatusRepository`. Provide visual feedback when a status is active.
    - [x] Implement a "Clear Status" button to wipe the active state.
- [x] **Step 2.3: Persistent Notification** 
    - [x] Implement a Foreground Service or `NotificationManager` utility.
    - [x] When a status is saved, post an ongoing notification showing the active status. Add a "Clear" action button directly on the notification.

## Phase 3: Telephony & SMS Integration
**Goal:** The core feature. Detect missed calls and send the SMS if a status is active.

- [x] **Step 3.1: Runtime Permissions Handling** 
    - [x] Add runtime permission request prompts to `MainActivity` for phone state, call logs, and SMS on first launch.
- [x] **Step 3.2: Call Detection (`CallReceiver`)** 
    - [x] Implement a `BroadcastReceiver` listening for `android.intent.action.PHONE_STATE`.
    - [x] Successfully distinguish between a truly missed call (state transitions from `RINGING` -> `IDLE`) and an answered call (`RINGING` -> `OFFHOOK`).
- [x] **Step 3.3: SMS Dispatcher** 
    - [x] Create a helper class to format the dynamic SMS string using the Time Calculator.
    - [x] Implement the dispatch mechanism using `android.telephony.SmsManager`.
- [x] **Step 3.4: Wiring the Flow** 
    - [x] In the Call Receiver, if a missed call is detected, check the `StatusRepository`.
    - [x] If active, construct the message and call the SMS Dispatcher to send the text to the incoming number.

## Phase 4: Reliability & Spam Prevention
**Goal:** Make the app robust for daily use by adding debounce logic and auto-expiry.

- [x] **Step 4.1: Debounce Logic** 
    - [x] Implement a lightweight logging system in `SharedPreferences` to record `[CallerNumber, Timestamp]`.
    - [x] Before sending an SMS, verify the incoming number hasn't received a message in the last X minutes (e.g., 10-15 min window).
- [x] **Step 4.2: Auto-Expiry Mechanism** 
    - [x] Integrate `AlarmManager`.
    - [x] When a status is set with a specific duration, schedule an alarm to clear the `StatusRepository` and dismiss the persistent notification exactly when the time is up.

## Phase 5: Polish & Accessibility
**Goal:** Streamline the user experience to meet the "under 5 seconds" activation success criteria.

- [x] **Step 5.1: Home Screen Widget** 
    - [x] Create an `AppWidgetProvider`.
    - [x] Allow users to activate predefined statuses (e.g., "Cricket - 2 Hours") with a single tap directly from the launcher without opening the app.
- [x] **Step 5.2: UI Refinements** 
    - [x] Polish the `MainActivity` design (Material 3). Add smooth transitions and handle edge cases in user input nicely.
- [x] **Step 5.3: Battery Optimization Guidance** 
    - [x] Add an in-app prompt or settings button to guide the user to disable Android battery optimization for Sensy, ensuring the Call Receiver isn't aggressively killed by the OS.

## Phase 6: Widget Redesign & Dynamic History (Proposed)
**Goal:** Make the widget look premium and allow flexible status selection without opening the app, while working around Android's strict widget limitations.

**The Android Limitation:** Android operating systems strictly forbid typing boxes (`EditText`) directly inside Home Screen Widgets. You cannot pop up a keyboard directly on the home screen itself.

**The Proposed Solution:**
- **Step 6.1: The "Recent History" Widget List**
  - Instead of hardcoding 2 buttons, we will convert the widget to use a scrollable `ListView`.
  - It will automatically display your 10 most recent statuses from the app. Whatever custom status and time you type once in the app will instantly become a one-tap button on your widget forever!
- **Step 6.2: Premium UI Redesign**
  - We will give the widget a modern, sleek design with rounded corners, a translucent background, and clean typography.
  - The top section will clearly highlight if a status is currently active (in green) with the remaining time.
- **Step 6.3: "New Custom" Shortcut**
  - We will add a small "+" button on the widget that instantly launches the app with the keyboard already open, so you can type a brand-new status in less than 2 seconds.
