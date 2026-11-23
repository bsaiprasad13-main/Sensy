package com.example.sensy

object MessageBuilder {

    /**
     * Sent to callers who are not in the user's contacts.
     * Deliberately reveals no name, activity or return time. Kept under 160 characters
     * so it goes out as a single SMS.
     */
    const val GENERIC_MESSAGE =
        "Hi, I can't take your call right now. If it's important, just text me what it's about and I'll get back to you."

    fun buildSmsMessage(statusText: String, startTimeMillis: Long, durationMinutes: Int): String {
        val timeCalculator = TimeCalculator()
        val remainingTimeMsg = timeCalculator.calculateRemainingTimeMessage(
            startTimeMillis,
            durationMinutes
        )
        return "Hi, This is Sensy, ${BuildConfig.USER_NAME}'s assistant. Right now he ${statusText.lowercase()}. $remainingTimeMsg Thank you."
    }

    /**
     * Picks the reply for a missed call.
     *
     * - Saved contact  -> personal message with the current status and time left.
     * - Unknown number -> generic message, or null (send nothing) when [skipUnknownNumbers] is on.
     */
    fun messageFor(
        isSavedContact: Boolean,
        skipUnknownNumbers: Boolean,
        statusText: String,
        startTimeMillis: Long,
        durationMinutes: Int
    ): String? = when {
        isSavedContact -> buildSmsMessage(statusText, startTimeMillis, durationMinutes)
        skipUnknownNumbers -> null
        else -> GENERIC_MESSAGE
    }
}
