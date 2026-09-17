package com.example.sensy.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.sensy.service.SmsDispatcherService

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            val number = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

            Log.d("CallReceiver", "Phone state changed: $state, number: $number")

            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                lastState = TelephonyManager.EXTRA_STATE_RINGING
                if (!number.isNullOrEmpty()) {
                    savedNumber = number
                }
            } else if (state == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                lastState = TelephonyManager.EXTRA_STATE_OFFHOOK
            } else if (state == TelephonyManager.EXTRA_STATE_IDLE) {
                // If it was ringing and went straight to idle, it's a missed call
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    if (!savedNumber.isNullOrEmpty()) {
                        Log.d("CallReceiver", "Missed call detected from $savedNumber")
                        handleMissedCall(context, savedNumber!!)
                    }
                }
                lastState = TelephonyManager.EXTRA_STATE_IDLE
                savedNumber = null // Reset for next call
            }
        }
    }

    private fun handleMissedCall(context: Context, incomingNumber: String) {
        // We will wire this up to the StatusRepository and SMS Dispatcher in Step 3.4
        val dispatcherIntent = Intent(context, SmsDispatcherService::class.java).apply {
            putExtra("incoming_number", incomingNumber)
        }
        context.startService(dispatcherIntent)
    }

    companion object {
        // Track the state across broadcasts
        private var lastState = TelephonyManager.EXTRA_STATE_IDLE
        private var savedNumber: String? = null
    }
}
