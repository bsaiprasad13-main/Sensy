package com.example.sensy.data

import android.content.Context
import android.content.SharedPreferences

class SmsLogRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun logSmsSent(phoneNumber: String) {
        prefs.edit().putLong(phoneNumber, System.currentTimeMillis()).apply()
    }

    fun canSendSms(phoneNumber: String, debounceWindowMinutes: Int = 15): Boolean {
        val lastSent = prefs.getLong(phoneNumber, 0L)
        if (lastSent == 0L) return true
        
        val elapsedTime = System.currentTimeMillis() - lastSent
        return elapsedTime > (debounceWindowMinutes * 60 * 1000L)
    }

    companion object {
        private const val PREFS_NAME = "sensy_sms_log_prefs"
    }
}
