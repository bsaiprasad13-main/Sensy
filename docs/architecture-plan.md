# Sensy: Automated Missed Call Status Assistant - Architecture Plan

## 1. High-Level Architecture Overview
The Sensy application follows a simple, localized, event-driven architecture. The system relies entirely on on-device processing, ensuring privacy, zero latency (no network calls), and high reliability. The architecture is divided into three primary layers:
1.  **Presentation Layer (UI):** Facilitates user interaction for setting and viewing status.
2.  **Data Layer:** Manages persistence of the current status and duration.
3.  **Background Processing & System Integration Layer:** Listens for system events (incoming calls) and interacts with system services (SMS).

```mermaid
graph TD
    User([User]) -->|Sets Status/Duration| UI[UI Layer: MainActivity / AppWidget]
    UI -->|Writes| Storage[(Local Storage: SharedPreferences)]
    
    System([Android Telephony System]) -.->|Incoming Call Event| Receiver[Call State Receiver]
    Receiver -->|Reads active status| Storage
    
    Receiver -->|If Missed Call + Active Status| Logic[Core Logic: Time Calculator]
    Logic -->|Validates| Debounce{Debounce Check}
    Debounce -->|Yes (Spam)| Drop[Do Nothing]
    Debounce -->|No (Safe)| Builder[Message Builder]
    
    Builder -->|Creates SMS| SMSAPI[SmsManager API]
    SMSAPI -.->|Dispatches| Caller([Caller])
```

## 2. Core Components

### 2.1. Presentation Layer (UI)
*   **MainActivity:** The primary screen containing:
    *   Preset Status Buttons (e.g., Cricket, Driving, Bath).
    *   Custom Status Text Input.
    *   Duration Selector (Presets + Custom + "No Limit").
    *   "Clear Status" button.
*   **AppWidgetProvider (Home Screen Widget):** Provides 1-tap activation for frequently used statuses.
*   **NotificationManager:** Displays a persistent foreground notification while a status is active, acting as a reminder and providing a quick action to "Clear" the status.

### 2.2. Data Layer
*   **SharedPreferences / DataStore:** Since the data footprint is extremely small, `SharedPreferences` (or AndroidX `DataStore`) is sufficient. No SQL database (like Room) is required.
    *   `ACTIVE_STATUS_TEXT` (String): e.g., "playing cricket".
    *   `START_TIMESTAMP` (Long): Epoch time when the status was set.
    *   `DURATION_MINUTES` (Int): Total duration in minutes (or -1 for no limit).
    *   `RECENT_CALLERS_LOG` (JSON/String): Used for debounce logic (Number + Timestamp).

### 2.3. Background Processing Layer
*   **Call Receiver (`BroadcastReceiver` or `CallScreeningService`):**
    *   Registers for `android.intent.action.PHONE_STATE` (simpler, legacy approach) or uses `CallScreeningService` (modern, robust approach for API 24+).
    *   *Optimization:* The receiver is registered in the AndroidManifest. It is awoken by the OS *only* when a call event occurs. It immediately checks if `ACTIVE_STATUS_TEXT` exists. If not, it halts execution immediately to conserve battery.
*   **Status Expiry Manager (`AlarmManager` or `WorkManager`):**
    *   Schedules an intent to automatically clear the `SharedPreferences` and dismiss the notification exactly when the duration expires.

### 2.4. Action & System Integration Layer
*   **Remaining Time Calculator:** A pure function that calculates: `RemainingTime = Duration - (CurrentTime - StartTime)`.
*   **Debounce Validator:** Checks `RECENT_CALLERS_LOG`. If the incoming number was texted within the last `X` minutes (e.g., 10 mins), it halts execution.
*   **SmsDispatcher:** Utilizes `android.telephony.SmsManager` to dispatch the generated text message.

## 3. Data Flows

### Flow 1: Setting a Status
1. User opens the App or taps the Widget.
2. User selects "Driving" for "30 mins".
3. App records `START_TIMESTAMP = [Now]`, `DURATION_MINUTES = 30`, `STATUS = "driving"` into `SharedPreferences`.
4. App fires an `AlarmManager` intent for `[Now] + 30 mins` to trigger auto-cleanup.
5. App posts a Persistent Notification showing the active status.

### Flow 2: Handling a Missed Call
1. System receives an incoming call. The user doesn't answer.
2. System broadcasts a Phone State change -> `CallReceiver` wakes up.
3. `CallReceiver` queries `SharedPreferences` for an active status.
    *   *If no status:* Exit immediately.
    *   *If status exists:* Proceed.
4. `CallReceiver` reads the incoming Caller ID.
5. `Debounce Validator` checks if Caller ID received an SMS recently.
    *   *If yes:* Exit immediately.
    *   *If no:* Proceed and log Caller ID + Timestamp.
6. `Time Calculator` determines the remaining minutes.
7. `Message Builder` formats the string: *"Hi, this is Sai's automated assistant. He is currently [driving] and will call you back in approx [12] minutes."*
8. `SmsDispatcher` calls `SmsManager.getDefault().sendTextMessage(...)`.

## 4. Permissions Strategy
The app requires sensitive permissions. As a personal/sideloaded app, these will be requested at runtime:
*   `READ_PHONE_STATE` / `READ_CALL_LOG`: To detect incoming calls and read the caller's number.
*   `SEND_SMS`: To dispatch the automated reply.
*   `POST_NOTIFICATIONS` (Android 13+): To display the persistent reminder notification.

## 5. Error Handling & Edge Cases

| Edge Case | Handling Strategy |
| :--- | :--- |
| **User Answers Call** | The `CallReceiver` must strictly distinguish between `RINGING` -> `OFFHOOK` (answered) and `RINGING` -> `IDLE` (missed). SMS is *only* sent on `IDLE` if it was previously `RINGING`. |
| **OS Kills App in Background** | Rely on manifest-registered `BroadcastReceivers` or `CallScreeningService` which are inherently awakened by OS events, rather than keeping a custom service running indefinitely. |
| **Duration Expires** | `AlarmManager` triggers a cleanup receiver that wipes `SharedPreferences` and cancels the persistent notification. |
| **Rapid Repeat Calls (Spam)** | The Debounce Validator maintains a lightweight map of `[PhoneNumber: LastMessagedTimestamp]`. Incoming calls are checked against this map before any SMS is constructed. |

## 6. Next Steps for Implementation
1. Initialize the Android Studio project (Kotlin).
2. Implement the isolated **Remaining Time Calculator** unit logic.
3. Scaffold the UI (MainActivity) and hook it up to `SharedPreferences`.
4. Implement the `BroadcastReceiver` to detect missed calls and verify `READ_PHONE_STATE` permissions.
5. Implement `SmsManager` and test sending on a real device.
6. Add the Debounce logic and Auto-Expiry feature.
