package com.example.sensy.ui.main

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import com.example.sensy.data.ActiveStatus
import com.example.sensy.data.RecentStatus
import com.example.sensy.data.StatusRepository
import com.example.sensy.receiver.AlarmReceiver
import com.example.sensy.data.PresetStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import android.content.SharedPreferences

class MainScreenViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = StatusRepository(application)

    private val _uiState = MutableStateFlow(MainScreenUiState())
    val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

    private val prefChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "status_text" || key == "start_time" || key == "duration_minutes") {
            loadStatus()
        }
    }

    init {
        loadStatus()
        repository.registerChangeListener(prefChangeListener)
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregisterChangeListener(prefChangeListener)
    }

    private fun loadStatus() {
        val active = repository.getActiveStatus()
        val recents = repository.getRecentStatuses()
        val presets = repository.getPresets()
        _uiState.value = MainScreenUiState(active, recents, presets)
    }

    fun addPreset(preset: PresetStatus) {
        repository.addPreset(preset)
        loadStatus()
    }

    fun removePreset(preset: PresetStatus) {
        repository.removePreset(preset)
        loadStatus()
    }

    fun removeRecentStatus(status: RecentStatus) {
        repository.removeRecentStatus(status)
        loadStatus()
        updateWidget()
    }

    fun saveStatus(statusText: String, durationMinutes: Int) {
        repository.saveStatus(statusText, durationMinutes)
        
        // Save to history so it shows in the recent list
        repository.saveRecentStatus(RecentStatus(statusText, durationMinutes))

        loadStatus()
        com.example.sensy.service.StatusService.start(getApplication())
        
        updateWidget()
        
        if (durationMinutes != -1) {
            scheduleAlarm(durationMinutes)
        } else {
            cancelAlarm()
        }
    }

    fun clearStatus() {
        repository.clearStatus()
        loadStatus()
        com.example.sensy.service.StatusService.stop(getApplication())
        cancelAlarm()
        updateWidget()
    }

    private fun updateWidget() {
        val app = getApplication<Application>()
        val intent = Intent(app, com.example.sensy.widget.SensyWidgetProvider::class.java).apply {
            action = android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE
        }
        val ids = android.appwidget.AppWidgetManager.getInstance(app).getAppWidgetIds(
            android.content.ComponentName(app, com.example.sensy.widget.SensyWidgetProvider::class.java)
        )
        intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        app.sendBroadcast(intent)
        
        // Also notify the list view to refresh its data
        android.appwidget.AppWidgetManager.getInstance(app).notifyAppWidgetViewDataChanged(ids, com.example.sensy.R.id.widget_list_view)
    }

    private fun scheduleAlarm(durationMinutes: Int) {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CLEAR_STATUS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            app, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val triggerTime = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } else {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } catch (e: SecurityException) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        }
    }

    private fun cancelAlarm() {
        val app = getApplication<Application>()
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(app, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_CLEAR_STATUS
        }
        val pendingIntent = PendingIntent.getBroadcast(
            app, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}

data class MainScreenUiState(
    val activeStatus: ActiveStatus? = null,
    val recentStatuses: List<RecentStatus> = emptyList(),
    val presets: List<PresetStatus> = emptyList()
)
