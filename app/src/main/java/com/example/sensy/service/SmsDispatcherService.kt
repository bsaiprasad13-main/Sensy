package com.example.sensy.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import android.util.Log
import com.example.sensy.TimeCalculator
import com.example.sensy.data.SmsLogRepository
import com.example.sensy.data.StatusRepository

class SmsDispatcherService : Service() {

    private lateinit var statusRepository: StatusRepository
    private lateinit var smsLogRepository: SmsLogRepository

    override fun onCreate() {
        super.onCreate()
        statusRepository = StatusRepository(this)
        smsLogRepository = SmsLogRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val incomingNumber = intent?.getStringExtra("incoming_number")
        if (!incomingNumber.isNullOrEmpty()) {
            if (!smsLogRepository.canSendSms(incomingNumber)) {
                Log.d("SmsDispatcher", "Debounced: Already sent SMS to $incomingNumber recently.")
                stopSelf()
                return START_NOT_STICKY
            }

            val activeStatus = statusRepository.getActiveStatus()
            if (activeStatus != null) {
                // Check if expired
                if (activeStatus.durationMinutes != -1) {
                    val elapsedTimeMs = System.currentTimeMillis() - activeStatus.startTimeMillis
                    val durationMs = activeStatus.durationMinutes * 60 * 1000L
                    if (elapsedTimeMs > durationMs) {
                        // Expired
                        statusRepository.clearStatus()
                        StatusService.stop(this)
                        stopSelf()
                        return START_NOT_STICKY
                    }
                }

                // Construct message
                val message = com.example.sensy.MessageBuilder.buildSmsMessage(
                    activeStatus.statusText,
                    activeStatus.startTimeMillis,
                    activeStatus.durationMinutes
                )
                sendSms(incomingNumber, message)
            }
        }
        stopSelf()
        return START_NOT_STICKY
    }

    private fun sendSms(phoneNumber: String, message: String) {
        try {
            val smsManager: SmsManager = this.getSystemService(SmsManager::class.java)
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            smsLogRepository.logSmsSent(phoneNumber)
            Log.d("SmsDispatcher", "SMS sent to $phoneNumber: $message")
        } catch (e: Exception) {
            Log.e("SmsDispatcher", "Failed to send SMS", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
