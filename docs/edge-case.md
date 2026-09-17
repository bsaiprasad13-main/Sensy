# Sensy: Automated Missed Call Status Assistant - Edge Cases & Mitigations

This document outlines potential edge cases that Sensy might encounter in real-world usage, along with the technical and UX strategies to handle them gracefully.

## 1. Call State & Telephony Edge Cases

| Edge Case | Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Answered Call Delay** | The user answers the call, but it rings for a few seconds first. The system must not mistake `RINGING -> OFFHOOK` for a missed call. | Ensure the `CallReceiver` strictly validates that the call state transitioned from `RINGING` directly to `IDLE`. If it goes to `OFFHOOK` (answered), abort the SMS flow immediately. |
| **Manually Rejected Calls** | The user actively declines an incoming call. | From the Android API perspective, a rejected call often looks identical to a missed call (`RINGING` -> `IDLE`). Sensy will treat this as a missed call and send the SMS. This is acceptable and expected behavior, as the user is still busy. |
| **Call Waiting (On another call)** | The user is already on a call, and a second call comes in and is missed. | The broadcast receiver will still fire. Sensy should handle this smoothly and dispatch the SMS to the second caller, informing them of the status. |
| **Dual SIM Devices** | The user has two active SIMs. | `SmsManager.getDefault()` uses the system default SIM for SMS. For Version 1, we rely on the system default. A future enhancement could query `SubscriptionManager` to reply via the specific SIM that received the call. |
| **Private / Unknown Callers** | An incoming call lacks a Caller ID. | The `CallReceiver` must check if the incoming phone number string is null or empty. If so, cleanly abort the operation because there is no destination to send the SMS to. |

## 2. Timing & Calculation Edge Cases

| Edge Case | Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Zero or Negative Remaining Time** | A call is missed exactly at or slightly after the status duration expires (before the auto-cleanup kicks in). | The `TimeCalculator` must never output negative minutes. If `RemainingTime <= 0`, substitute the SMS template with: *"Sai should be free now, he will call you back shortly."* |
| **"No Limit" Status** | The user activates a status without an end time. | Bypass the time calculation entirely. The SMS template should switch to a static message: *"Sai is currently [activity] and cannot answer the phone right now."* |
| **Overwriting Active Status** | The user sets a new status ("Driving") while an old one ("Cricket") is still active. | Overwrite the `SharedPreferences` values immediately, update the `START_TIMESTAMP` to now, and cancel/re-register the auto-expiry `AlarmManager` intent to reflect the new duration. |

## 3. Battery, OS & Lifecycle Edge Cases

| Edge Case | Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **OS Kills Background App / Doze Mode** | Android's aggressive battery management prevents the app from waking up on a call. | Provide prominent in-app instructions for the user to disable battery optimization specifically for Sensy. Additionally, use a manifest-declared `BroadcastReceiver` (or `CallScreeningService`), which Android inherently prioritizes waking up during telephony events. |
| **Device Reboot** | The phone is restarted while a status is active. | `AlarmManager` intents are cleared on reboot. We need a `BOOT_COMPLETED` receiver. When the phone boots, check `SharedPreferences`. If a status is active and time remains, re-register the alarm. If time has expired, clear the status. |
| **Network Unreachable / SMS Failure** | The phone has no cellular signal to dispatch the SMS. | This is outside the app's immediate control. We will call `SmsManager`, but the OS message queue will handle delivery retries once the signal is restored. |

## 4. Spam & Debounce Edge Cases

| Edge Case | Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Rapid Repeat Calls** | The same worried caller calls 3 times in 5 minutes. | The Debounce Validator checks the `RECENT_CALLERS_LOG`. If the `CallerNumber` exists with a timestamp less than 10-15 minutes ago, drop the execution entirely before calculating time or building strings. |
| **Multiple Callers at Once** | Caller A and Caller B call simultaneously. | Standard `BroadcastReceiver` processing is synchronous. Ensure `SharedPreferences` reads/writes for the Debounce log are thread-safe or atomic to prevent race conditions when updating the log. |

## 5. Permissions Edge Cases

| Edge Case | Description | Mitigation Strategy |
| :--- | :--- | :--- |
| **Permissions Revoked Mid-Use** | The user goes into system settings and revokes SMS or Phone State permissions. | The `CallReceiver` must wrap its execution in a permissions check (`ContextCompat.checkSelfPermission`). If permission is missing, fail silently and gracefully. `MainActivity` should prominently display a warning banner if permissions are missing when the user opens the app. |
