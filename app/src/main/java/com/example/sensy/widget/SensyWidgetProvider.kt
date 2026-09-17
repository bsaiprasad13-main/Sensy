package com.example.sensy.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import com.example.sensy.MainActivity
import com.example.sensy.R
import com.example.sensy.data.StatusRepository
import com.example.sensy.receiver.AlarmReceiver
import com.example.sensy.service.StatusService

class SensyWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val repository = StatusRepository(context)

        if (intent.action == ACTION_CLEAR || intent.action == ACTION_LIST_CLICK) {
            
            if (intent.action == ACTION_CLEAR) {
                repository.clearStatus()
                StatusService.stop(context)
                
                // Cancel Alarm
                val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = AlarmReceiver.ACTION_CLEAR_STATUS
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
                if (pendingIntent != null) {
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    alarmManager.cancel(pendingIntent)
                }
            } else if (intent.action == ACTION_LIST_CLICK) {
                val statusText = intent.getStringExtra(EXTRA_STATUS_TEXT) ?: return
                val durationMins = intent.getIntExtra(EXTRA_DURATION_MINS, 15)
                
                repository.saveStatus(statusText, durationMins)
                StatusService.start(context)
                
                if (durationMins != -1) {
                    val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                        action = AlarmReceiver.ACTION_CLEAR_STATUS
                    }
                    val pendingIntent = PendingIntent.getBroadcast(
                        context, 0, alarmIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + (durationMins * 60 * 1000L),
                        pendingIntent
                    )
                }
            }

            // Trigger UI update
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, SensyWidgetProvider::class.java)
            val allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)
            for (widgetId in allWidgetIds) {
                updateAppWidget(context, appWidgetManager, widgetId)
            }
        }
    }

    companion object {
        const val ACTION_CLEAR = "com.example.sensy.WIDGET_CLEAR"
        const val ACTION_LIST_CLICK = "com.example.sensy.WIDGET_LIST_CLICK"
        const val EXTRA_STATUS_TEXT = "extra_status_text"
        const val EXTRA_DURATION_MINS = "extra_duration_mins"

        internal fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val repository = StatusRepository(context)
            val activeStatus = repository.getActiveStatus()
            val views = RemoteViews(context.packageName, R.layout.widget_sensy)

            if (activeStatus != null) {
                val durationStr = if (activeStatus.durationMinutes == -1) "No Limit" else "${activeStatus.durationMinutes}m"
                views.setTextViewText(R.id.widget_active_status, "Active: ${activeStatus.statusText} ($durationStr)")
                views.setTextColor(R.id.widget_active_status, android.graphics.Color.parseColor("#4CAF50")) // Green
            } else {
                views.setTextViewText(R.id.widget_active_status, "No active status")
                views.setTextColor(R.id.widget_active_status, android.graphics.Color.parseColor("#888888")) // Grey
            }

            // Buttons
            views.setOnClickPendingIntent(R.id.btn_clear, getPendingIntent(context, ACTION_CLEAR))
            
            // New App Launch Intent
            val launchIntent = Intent(context, MainActivity::class.java)
            val launchPendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_new_status, launchPendingIntent)

            // Setup List
            val serviceIntent = Intent(context, WidgetRemoteViewsService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_list_view, serviceIntent)
            views.setEmptyView(R.id.widget_list_view, R.id.widget_empty_view)

            // Setup List Click Template
            val listClickIntent = Intent(context, SensyWidgetProvider::class.java).apply {
                action = ACTION_LIST_CLICK
            }
            val listClickPendingIntent = PendingIntent.getBroadcast(
                context, 1, listClickIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_list_view, listClickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list_view)
        }

        private fun getPendingIntent(context: Context, action: String): PendingIntent {
            val intent = Intent(context, SensyWidgetProvider::class.java).apply {
                this.action = action
            }
            return PendingIntent.getBroadcast(context, 2, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }
}
