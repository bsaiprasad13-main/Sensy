package com.example.sensy

class TimeCalculator {
    /**
     * Calculates the remaining time in milliseconds.
     *
     * @param startTimeMillis The timestamp when the status started.
     * @param durationMinutes The duration in minutes. A negative value represents "no limit".
     * @param currentTimeMillis The current time in milliseconds. Defaults to System.currentTimeMillis().
     * @return The remaining time in milliseconds. Returns -1 if there is "no limit". Returns 0 if time is up.
     */
    fun calculateRemainingTimeMillis(
        startTimeMillis: Long,
        durationMinutes: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Long {
        if (durationMinutes < 0) {
            return -1L // No limit
        }
        if (durationMinutes == 0) {
            return 0L // Immediately expired
        }
        
        val durationMillis = durationMinutes * 60 * 1000L
        val endTimeMillis = startTimeMillis + durationMillis
        val remaining = endTimeMillis - currentTimeMillis
        
        return if (remaining > 0) remaining else 0L
    }
    
    /**
     * Checks if the given status time has expired.
     * 
     * @param startTimeMillis The timestamp when the status started.
     * @param durationMinutes The duration in minutes. A negative value represents "no limit".
     * @param currentTimeMillis The current time in milliseconds. Defaults to System.currentTimeMillis().
     * @return True if the status has expired, false otherwise.
     */
    fun isExpired(
        startTimeMillis: Long,
        durationMinutes: Int,
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (durationMinutes < 0) return false
        return calculateRemainingTimeMillis(startTimeMillis, durationMinutes, currentTimeMillis) == 0L
    }

    /**
     * Calculates a human-readable remaining time message.
     */
    fun calculateRemainingTimeMessage(
        startTimeMillis: Long,
        durationMinutes: Int
    ): String {
        if (durationMinutes < 0) {
            return "He will be back soon."
        }
        val remainingMillis = calculateRemainingTimeMillis(startTimeMillis, durationMinutes)
        if (remainingMillis == 0L) {
            return "He should be available now."
        }
        
        val remainingMinutes = remainingMillis / (60 * 1000L)
        return "He will be back in $remainingMinutes mins."
    }
}
