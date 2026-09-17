# Problem Statement: Sensy, an Automated Missed Call Status Assistant

**App name: Sensy**
The name is inspired by the Japanese word for teacher or guide, a quiet presence that watches out for you and handles things on your behalf while you're away, without needing to be asked. Package name reference going forward: `com.saiprasad.sensy`.

## 1. The Problem

When I keep my phone aside for an activity like playing cricket, taking a bath, or driving, I have no way to tell someone who calls me during that time why I am not picking up or when I will call them back. The caller is left guessing. They do not know if I am ignoring them, if my phone is off, or if I am simply busy and will return their call soon.

This creates two issues:
- The caller is left without information, which can cause unnecessary worry or frustration, especially if the matter is time sensitive.
- I come back to a missed call with no context on whether I need to act fast or can call back at leisure, and the caller has no idea when to expect a callback.

## 2. Why This Matters

This is a small but real gap in a daily habit almost everyone has. People already use tools like WhatsApp's "away" status or email auto-replies to solve the same problem in other channels. Phone calls, the most interruptive and urgent communication channel, have no equivalent. A caller today either lets the phone ring out into silence or lands on a generic, unhelpful voicemail greeting that never changes.

Solving this well means building a small piece of software that acts like a personal assistant, one that knows my current activity and communicates it automatically, without me lifting a finger once it is set.

## 3. Why We Considered and Ruled Out Other Approaches

Before settling on the final approach, three options were considered. Documenting why each was accepted or rejected is important, since it explains the reasoning behind the final design, not just the design itself.

**Option A: Real time call screening with voice message**
The idea was to intercept the incoming call using Android's CallScreeningService API and play an automated voice message to the caller live, before the call even rings, similar to Google's Call Screen feature. This was rejected as the primary path because injecting custom audio into an active call is not natively supported by Android in a reliable way. It would need deep telecom framework access that is inconsistent across phone brands and Android versions. This made it too fragile for something meant to be used every day.

**Option B: Dynamic voicemail greeting**
The idea was to update my voicemail greeting automatically based on my status, so a caller who reaches voicemail hears the current message. This was rejected because voicemail in India is controlled entirely by the carrier's server side systems (Jio, Airtel, Vi), not by the phone or any app. None of the major Indian carriers expose a public API to update greetings programmatically. Without that access, this approach cannot be automated at all.

**Option C: SMS auto reply on missed call (chosen)**
This approach detects a missed call using Android's own call state APIs and immediately sends an SMS to the caller with my current status and expected callback time. It was chosen because:
- It needs no carrier cooperation, since SMS sending is a standard, well supported Android API.
- It needs no live audio handling, so it avoids the biggest reliability risk from Option A.
- It reaches the caller almost immediately after the missed call, faster than waiting through a voicemail flow.
- It works the same way regardless of what phone or carrier the caller has, since it is a plain text message.
- It is realistic to build, test, and maintain by one person, with no ongoing subscription cost.

## 4. Objectives

- Let me set a status (for example "Playing cricket") with an optional duration (for example 30 minutes, or no limit if unknown) in under 5 seconds, through a home screen widget or shortcut.
- When a call is missed while a status is active, automatically send the caller an SMS stating my status and expected callback time.
- Avoid sending duplicate messages to the same caller within a short window.
- Automatically expire the status once the set duration passes, so outdated messages are not sent after I am back.
- Keep the whole system running reliably on my own phone, with no dependency on third party paid services.

## 5. Scope

**In scope for version 1**
- Manual status setting with quick preset buttons (Cricket, Driving, Bath) for speed, plus a Custom option where I can type any activity freely. Presets are shortcuts only, never a restriction.
- A duration picker with common presets (15 min, 30 min, 1 hr) plus a custom option to type any number of minutes, or a "no limit" option if the duration is unknown.
- Missed call detection using Android call state APIs.
- Automatic SMS reply to the caller with the status message and a live, recalculated remaining time, not the original duration.
- Debounce logic to avoid repeat messages to the same number within a set time window.
- Automatic status expiry after the set duration.
- A persistent notification while a status is active, as a reminder to turn it off if I return early.

**Out of scope for version 1**
- Real time call screening or voice messages during the call.
- Automatic status detection through sensors, location, or calendar. Status is set manually for now.
- Urgency override features that make the phone ring through silent mode.
- Support for iOS, since the required call handling APIs are Android specific.

## 6. Key Design Considerations and Why They Matter

- **Speed of setting status**: if setting a status takes more than a few seconds, I will not bother doing it before a bath or a match, and the whole system becomes useless in practice. A one tap widget is prioritized over a full app flow.
- **On device text to speech is not needed here**: since this is a text message, not a voice message, there is no dependency on any speech engine, which removes a major point of failure from the earlier options.
- **Debounce logic checked early, before any calculation**: without this, if someone calls three times in five minutes because they are worried, they would get three identical texts, which feels robotic and annoying rather than helpful. The debounce check should run right after confirming a status is active, before the remaining time is calculated or the message is built, so repeat calls are rejected quickly without unnecessary work.
- **Call listener stays registered at all times, but stays idle when there is nothing to do**: the listener itself costs almost no battery sitting idle, since Android only wakes it up when an actual call event happens. Rather than trying to switch it on and off based on whether a status is active, which risks timing issues, the listener stays registered permanently, and simply checks a flag the moment it wakes up, is a status active. If not, it exits immediately with no further work. This keeps battery impact minimal without adding fragility.
- **Status auto expiry**: if I forget to turn off a status, callers after I am already free would get a false message saying I am still busy, which defeats the purpose and could even cause confusion or mistrust in the tool.
- **Permissions**: Android treats call log access and SMS sending as sensitive permissions. Since Sensy is for personal use only and will be sideloaded rather than published, this is manageable, but it is worth knowing in case the project is ever shared or published later.
- **No fixed status options**: presets like Cricket or Bath exist only to make repeat entries faster, since those are the activities I will log the most. A custom text field is always available so the status can describe anything, this keeps the tool flexible instead of forcing my day into a fixed menu.
- **Dynamic remaining time, not a static repeat**: if I set a 30 minute status, the app does not just remember "30 minutes" and repeat that number to every caller. It stores the time the status started and the total duration, then calculates remaining time fresh at the moment each call is missed, using current time minus start time, subtracted from total duration. A caller at the very end of the window hears the task is finishing, a caller ten minutes in hears an accurate lower number, not the original 30. This matters because a static repeated number would actively mislead callers the later they call into the window.

## 7. How to Implement This Further

**Phase 1: Core status and messaging flow**
- Build a simple Android app with a status picker screen (preset buttons plus a Custom free text entry) and a duration selector (presets plus custom minutes plus a no limit option).
- Store the active status text, the time the status started, and the total duration (or a flag for no limit) locally on the device.
- Implement a CallScreeningService (or PhoneStateListener as a simpler starting point) to detect missed calls.
- Build a small function that runs at the moment of each missed call: it takes current time minus start time, subtracts that from total duration, and returns the remaining time. This runs fresh every time, so two different callers at two different moments in the same status window get two different, accurate remaining time values.
- On a missed call, first check if a status is active, then immediately check whether this exact number has already been texted within the debounce window, before doing any calculation. Only if both checks pass, calculate the remaining time, generate the SMS text (or a generic "busy right now" message if no limit was set, or a "should be free now" message if remaining time has reached zero), and send it using Android's SmsManager.

**Phase 2: Reliability and edge cases**
- Add debounce logic, do not resend to the same number within a configurable window, for example 10 minutes.
- Add auto expiry logic that clears the status once the timer runs out, and cancels the reminder notification.
- Handle the edge case of a call that is answered on a second ring, so a normal answered call never accidentally triggers a status SMS.
- Test across different call scenarios on my own phone directly, not in an emulator, since real call handling behavior is what matters.

**Phase 3: Convenience and polish**
- Add a home screen widget for one tap status setting.
- Add a persistent notification showing the active status and a quick "I'm back" button to clear it manually.
- Refine the SMS message template to sound natural and not robotic.
- Review and disable battery optimization for Sensy so Android does not kill the background service, and confirm the app is set as needed for call screening access.

**Phase 4: Review and maintain**
- Use Sensy daily for a couple of weeks and note any missed detections or false triggers.
- Revisit after any major Android OS update on my phone to confirm permissions and background behavior still work as expected, roughly once or twice a year.

## 8. Success Criteria

- A status can be set in under 5 seconds through the widget.
- A missed call while a status is active reliably results in an SMS to the caller within a few seconds.
- No duplicate messages are sent for repeated calls within the debounce window.
- The status clears itself automatically once its duration passes, with no manual step needed.
- The system runs for weeks without needing to be manually restarted or fixed.

## 9. Next Steps

- Set up a new Android project (Kotlin, using Android Studio conventions) inside Antigravity, and confirm call state and SMS permissions can be requested and granted on the actual test device before writing any other logic.
- Build the remaining time calculation function first, in isolation, and test it with sample start times and durations before wiring it into the call detection flow, since this is the core logic the whole app depends on getting right.
- Build the status picker screen next (presets, custom text, custom duration, no limit option), then connect it to local storage.
- Build the missed call detection and SMS sending flow, and test with real calls from a second phone.
- Add debounce and auto expiry once the core flow is confirmed working.
- Add the widget and persistent notification last, once the underlying logic is solid.

## 10. Cost

- Development cost: none beyond personal time, built using Android Studio.
- Running cost: SMS is sent through the phone's own SIM using the standard SmsManager API, so cost is whatever the carrier charges per SMS, which is negligible or free under most Indian unlimited texting plans.
- No cloud services, no third party APIs, no subscription costs.
- Only cost is occasional time spent revisiting the app after major Android updates.
