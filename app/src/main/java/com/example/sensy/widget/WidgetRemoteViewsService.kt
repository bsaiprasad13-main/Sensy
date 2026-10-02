package com.example.sensy.widget

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.sensy.R
import com.example.sensy.data.RecentStatus
import com.example.sensy.data.StatusRepository

class WidgetRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return WidgetRemoteViewsFactory(this.applicationContext)
    }
}

class WidgetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {
    private var historyList: List<RecentStatus> = emptyList()

    override fun onCreate() {
        // Initialization if needed
    }

    override fun onDataSetChanged() {
        // This is triggered when notifyAppWidgetViewDataChanged is called
        val repository = StatusRepository(context)
        historyList = repository.getRecentStatuses()
    }

    override fun onDestroy() {
        historyList = emptyList()
    }

    override fun getCount(): Int {
        return historyList.size
    }

    override fun getViewAt(position: Int): RemoteViews {
        if (position >= historyList.size) return RemoteViews(context.packageName, R.layout.widget_list_item)
        
        val status = historyList[position]
        val rv = RemoteViews(context.packageName, R.layout.widget_list_item)

        rv.setTextViewText(R.id.item_status_text, status.text)
        val durationStr = if (status.durationMinutes == -1) "No Limit" else "${status.durationMinutes}m"
        rv.setTextViewText(R.id.item_duration_text, durationStr)

        // Set the fill-in intent so the provider can catch the click
        val fillInIntent = Intent().apply {
            putExtra(SensyWidgetProvider.EXTRA_STATUS_TEXT, status.text)
            putExtra(SensyWidgetProvider.EXTRA_DURATION_MINS, status.durationMinutes)
        }
        rv.setOnClickFillInIntent(R.id.widget_list_item_root, fillInIntent)
        rv.setOnClickFillInIntent(R.id.item_status_text, fillInIntent)
        rv.setOnClickFillInIntent(R.id.item_duration_text, fillInIntent)

        return rv
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
