
package com.nothing.musicwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LEVEL = "com.nothing.musicwidget.AUDIO_LEVEL"
        const val EXTRA_LEVEL = "audio_level"
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (id in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)
            appWidgetManager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: android.content.Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_LEVEL) {
            val level = intent.getIntExtra(EXTRA_LEVEL, 0)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(
                    context,
                    MusicWidgetProvider::class.java
                )
            )

            for (id in ids) {
                val views = RemoteViews(context.packageName, R.layout.music_widget)
                views.setTextViewText(
                    R.id.audio_level,
                    "LIVE  •  $level%"
                )
                manager.updateAppWidget(id, views)
            }
        }
    }
}
