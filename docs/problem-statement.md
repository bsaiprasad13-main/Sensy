# Sensy: Automated Missed Call Status Assistant - Detailed Problem Statement & Project Context

## Executive Summary
Sensy (`com.saiprasad.sensy`) is a personal Android assistant designed to solve a specific communication gap: informing callers of your status and expected callback time when you are unable to answer the phone (e.g., while playing a sport, driving, or taking a bath). Unlike standard, unhelpful voicemail or unanswered ringing, Sensy acts proactively by sending an automated, dynamically calculated SMS to the caller, providing immediate context and reducing frustration.

## 1. The Core Problem
When a user is temporarily separated from their phone or engaged in an activity where they cannot answer calls, incoming callers are left without context. They do not know:
- Why the call is unanswered.
- Whether the user is ignoring the call, the phone is off, or the user is simply busy.
- When they can expect a return call.

This lack of information creates frustration and worry for the caller, especially for urgent matters. For the user, it results in returning to missed calls without knowing the urgency or managing caller expectations. While messaging apps (like WhatsApp) and emails have "away" statuses or auto-replies, standard cellular phone calls lack this critical, immediate context.

## 2. The Solution: Sensy
Sensy bridges this gap by functioning as a quiet, automated personal assistant. The solution is an on-device Android application that allows the user to set a temporary "status" (with an optional duration). If a call is missed while the status is active, Sensy automatically replies to the caller via SMS with the user's current activity and a dynamically calculated time until they are free.

### Why SMS over other methods?
During the design phase, alternative approaches like real-time voice call screening and dynamic voicemail greetings were considered. They were rejected because:
- **Call Screening with Voice:** Injecting custom audio into an active call is unreliable and lacks native, consistent Android support across different manufacturers.
- **Dynamic Voicemail:** Voicemail greetings in India are controlled entirely by carrier systems (Jio, Airtel, Vi) without public APIs for programmatic updates.

**SMS Auto-Reply** was chosen because it relies on standard, reliable Android APIs (`SmsManager`), requires no carrier API integration or internet connectivity, delivers information faster than voicemail, and works universally regardless of the caller's device or network.

## 3. Key Objectives & Scope
The primary objective is to build a reliable, fast-to-use, battery-efficient application running locally on the user's Android device.

**In-Scope Features:**
- **Rapid Status Configuration:** Setting a status must take under 5 seconds to encourage regular use. This includes preset activities (Cricket, Driving, Bath), custom text entry, and flexible duration options (presets, custom minutes, or 'no limit').
- **Missed Call Detection:** Utilizing Android's `CallScreeningService` or `PhoneStateListener` to reliably identify unanswered calls in the background.
- **Dynamic SMS Auto-Reply:** Calculating the exact remaining time dynamically (Current Time - Start Time subtracted from Total Duration) rather than sending a static, inaccurate duration to late callers.
- **Debounce Logic:** Preventing spam by rejecting duplicate messages to the same number within a configurable window (e.g., 10 minutes). This check runs immediately upon detecting a call, minimizing unnecessary processing.
- **Automatic Status Expiry:** Clearing the active status once the set duration expires to prevent sending stale "busy" messages.
- **Persistent Notification:** Displaying an ongoing notification while the status is active, allowing the user to manually clear it early (e.g., an "I'm back" button).

**Out of Scope for Initial Version:**
- Real-time voice messages during calls.
- Automated status detection (via sensors, calendar, or location).
- Urgency overrides (forcing the phone to ring on silent).
- iOS support (due to fundamental differences in OS-level call handling).

## 4. Technical Architecture
Sensy is designed with a simple, robust architecture focusing on privacy and zero reliance on cloud services. It consists of four main components interacting locally:

1. **User Interface (UI):** Built in Kotlin, this includes the status picker, duration selector, and eventually a home screen widget (`AppWidgetProvider`) for one-tap activation.
2. **Local Storage:** The app's memory (using `SharedPreferences` or similar) to store the active status text, the exact start time, and the duration. It does not store fixed end times, ensuring robust calculations.
3. **Background Call Listener:** A permanently registered background service (`CallScreeningService` or `TelephonyManager`) that monitors for incoming calls. To conserve battery, it remains completely idle until an actual call event wakes it up. Upon waking, it immediately checks a simple active-status flag before executing further logic.
4. **Message Sender & Calculator:** Upon a missed call, this component queries local storage, validates the debounce criteria, calculates the accurate remaining time, constructs the message (e.g., "Sai is playing cricket and will call back in around 20 minutes"), and dispatches it via `SmsManager`.

## 5. Technical Challenges & Mitigations
- **Aggressive Battery Optimization:** Android may kill background services. *Mitigation:* Guide the user to manually disable battery optimization for Sensy and employ recommended background service patterns. The listener is kept permanently registered but idle to ensure it is always ready without draining power.
- **Manufacturer Restrictions:** Custom UI skins (e.g., Xiaomi, Oppo) aggressively block call listeners. *Mitigation:* Conduct primary testing directly on the target physical device rather than an emulator to accommodate OEM-specific quirks.
- **Accidental Triggers:** Ensuring the app doesn't send a text if the user answers the call on the second ring. *Mitigation:* Strict validation of call states to differentiate between a brief ring and a truly missed call.
- **Calculation Accuracy:** *Mitigation:* The core remaining-time calculation function is isolated and tested independently before integrating it with call detection APIs.

## 6. Success Criteria
The project is considered successful when:
1. A status can be activated via a widget in under 5 seconds.
2. An SMS containing accurate, dynamically calculated remaining time is reliably sent within seconds of a missed call.
3. Debounce logic successfully prevents duplicate SMS messages.
4. The system automatically cleans up expired statuses without manual intervention.
5. The application operates continuously for weeks without crashing, draining the battery, or requiring manual restarts.
6. The entire ecosystem functions with zero ongoing financial cost (aside from standard carrier SMS rates) and zero cloud dependency.
