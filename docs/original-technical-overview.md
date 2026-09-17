# Technical Overview: Sensy, the Missed Call Status App

**App name: Sensy** (package name reference: `com.saiprasad.sensy`)

This document explains how Sensy works under the hood, in plain terms, so you have a full mental model before building anything in Antigravity.

## 1. What "Architecture" Means Here

Architecture just means how the different parts of the app are organized and how they talk to each other. Think of it like the floor plan of a house, before you worry about paint colors or furniture, you need to know where the rooms are and how they connect.

This app has four main parts:

**Part 1: The UI (what you see and tap)**
This is the status picker screen, the preset buttons, the custom text field, the duration picker, and later the widget and notification. This is the only part you directly interact with.

**Part 2: Local storage (the app's memory)**
This is where the app remembers your current status, when it started, and how long it should last. It lives entirely on your phone, nothing is sent to any server. Think of it as a small notebook the app keeps for itself.

**Part 3: The call listener (the background watcher)**
This is a piece of code that runs quietly in the background, even when you're not looking at the app, watching for incoming calls and noticing when one is missed. This is the part that needs special Android permissions, since it's watching something sensitive, your call activity.

**Part 4: The message sender (the responder)**
Once the call listener notices a missed call, this part does two things, it asks the remaining time calculator how much time is left, then builds a message and sends it as an SMS using your phone's own texting system.

So the full picture is, you set something in the UI, it gets saved to local storage, the call listener watches in the background, and when it detects a missed call, it reads from storage, calculates remaining time, and triggers the message sender. No internet connection is needed for any of this, and no data ever leaves your phone.

## 2. Tech Stack, and Why Each Piece Is Used

**Kotlin**: the programming language the whole app is written in. It's Android's default language today, well supported by Antigravity, and has clean syntax.

**Android Studio project structure**: even inside Antigravity, the underlying project follows standard Android app structure, since Antigravity builds on top of the same Android SDK and tools.

**Android SDK components we'll use specifically**:
- `SharedPreferences` or a small local database (Room), for storing the status, start time, and duration. For something this small, SharedPreferences alone is likely enough, a full database is probably overkill.
- `CallScreeningService` or `PhoneStateListener` / `TelephonyManager`, this is the call listener part, the background watcher for missed calls.
- `SmsManager`, this is what actually sends the SMS from your phone's own number, no external service involved.
- `AlarmManager` or a simple background timer, for handling status auto expiry once the duration runs out.
- `AppWidgetProvider`, for the home screen widget, once we get to that milestone.

**No cloud services, no APIs, no backend server.** Everything runs and stores data locally on your device. This is a deliberate choice, it keeps the app fast, free to run, private, and independent of any internet connection.

## 3. How the Flow Works, Step by Step

Here is a full walkthrough of a real scenario, from start to finish.

**Step 1**: You open the app or tap the widget. You select "Playing cricket" or type a custom status, and set a duration, say 30 minutes.

**Step 2**: The app records three things in local storage, the status text, the exact time right now (the start time), and the duration you chose. It does not store "will end at 3:30," it stores the start time and the duration separately, and calculates the end point fresh whenever it needs it, this is more reliable than storing a fixed end time.

**Step 3**: The app shows a persistent notification confirming the status is active, so you have a visual reminder it's running.

**Step 4**: You put your phone aside and go play.

**Step 5**: Someone calls you. Since you don't answer, the call goes unanswered, and the call listener, which has been quietly running in the background this whole time, detects this missed call event along with the caller's phone number.

**Step 6**: The message sender wakes up. It first checks local storage, is there an active status right now. If no, it stops immediately, nothing further happens.

**Step 7**: If a status is active, it immediately checks one more thing before doing any real work, have I already texted this exact number in the last several minutes. If yes, it stops right here too, no calculation, no message building, nothing. This check is deliberately placed early, before any calculation, so repeated calls from the same number get rejected quickly with minimal processing, rather than doing the full calculation and message building first and only then deciding not to send.

**Step 8**: Only if both checks above pass, it runs the remaining time calculation, current time minus start time, subtracted from total duration.

**Step 9**: Based on the result, it builds an appropriate message. If there's meaningful time left, something like "Hey, this is Sai's assistant, he's playing cricket right now and will call you back in around 20 minutes." If time is up, something like "Hey, Sai should be free now, he'll call you back shortly."

**Step 10**: It sends the SMS using your phone's own SmsManager, exactly like a normal text message, just triggered automatically instead of typed by hand.

**Step 11**: Once the total duration passes, the app automatically clears the status, removes the persistent notification, and from that point, missed calls no longer trigger any message, since there's nothing active to report.

**Step 12**: If you come back early, you can tap "I'm back" on the notification to clear the status manually before the timer even finishes.

## 4. Problems We May Face, and How We're Tackling Them

**Problem: Android kills background services to save battery**
Android is aggressive about shutting down apps running in the background, which could stop the call listener from working when you actually need it.
*How we tackle it*: we'll guide you to manually disable battery optimization for this specific app in your phone's settings, and use Android's recommended patterns for long-running background services so the system is less likely to kill it.

**Problem: Wanting to reduce battery use by only running the listener when a status is active**
A natural instinct is to register the call listener only while a status is active, and unregister it the rest of the time.
*How we tackle it*: this isn't actually necessary, and doing it that way adds risk. `CallScreeningService` is event driven, Android itself wakes it up only when a call actually rings, so it costs almost no battery sitting idle regardless of whether a status is active. Instead of toggling registration on and off, which risks the listener not being ready in time for the very next call, we keep it registered permanently, and have it check a simple flag the moment it wakes up, is a status active right now. If not, it exits immediately and does nothing further. This gets the same low battery impact without the added risk of a listener that isn't reliably in place.

**Problem: Some phone brands restrict call related background access**
Certain manufacturers, like Xiaomi, Oppo, or Vivo, add extra restrictions on top of standard Android, which can block call listening apps more aggressively than stock Android does.
*How we tackle it*: we'll test directly on your actual phone early, rather than assuming standard Android behavior, and adjust settings or permissions specific to your phone's brand if needed.

**Problem: Permissions can feel invasive**
The app needs to read call state and send SMS, both considered sensitive permissions by Android.
*How we tackle it*: since this is for your personal use only, sideloaded rather than published on the Play Store, this isn't a blocker, just something to explain clearly when you request the permissions on first launch.

**Problem: Duplicate messages if someone calls repeatedly**
Without a safeguard, a worried caller who calls three times in five minutes could get three identical texts.
*How we tackle it*: the debounce logic in Step 7 above, no repeat message to the same number within a set window, for example ten minutes. This check is placed early, right after confirming a status is active and before any calculation happens, so repeated calls are rejected quickly with minimal processing rather than doing the full calculation first.

**Problem: Stale status after you're already back**
If the status isn't cleared, someone calling after you're already free would get a message saying you're still busy.
*How we tackle it*: automatic expiry once the duration passes, plus a manual "I'm back" option for early returns, and a persistent notification so you're never unsure whether a status is still active.

**Problem: Wrongly triggering during answered calls**
If you pick up on a second ring, we don't want the system to mistakenly treat that as missed and send a text mid conversation.
*How we tackle it*: careful handling of call states, distinguishing a truly missed call from a brief ring before you answer, and thorough testing of this specific case before relying on the app daily.

**Problem: Remaining time being wrong if calculated incorrectly**
Since the whole point of the upgraded design is accurate live remaining time, a bug here would undermine the entire idea.
*How we tackle it*: this function gets built and tested completely on its own first, before it's ever connected to real calls, exactly as laid out in Milestone 2 of your build plan.

## 5. Summary

The core idea is simple even though there are several moving pieces, you set a status, the phone remembers when it started and for how long, a background watcher notices missed calls, and a message gets sent with a freshly calculated remaining time. Everything happens locally on your phone, nothing needs the internet, and each risk has a specific, testable fix built into the plan rather than being left to chance.
