
package com.nothing.musicwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

class MusicWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_LEVEL = "com.nothing.musicwidget.AUDIO_LEVEL"
        const val EXTRA_LEVEL = "audio_level"

        private val BAR_IDS = intArrayOf(
            R.id.bar1, R.id.bar2, R.id.bar3, R.id.bar4,
            R.id.bar5, R.id.bar6, R.id.bar7, R.id.bar8,
            R.id.bar9, R.id.bar10, R.id.bar11, R.id.bar12
        )
    }

    override fun onUpdate(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)
            manager.updateAppWidget(id, views)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action != ACTION_LEVEL) return

        val level = intent.getIntExtra(EXTRA_LEVEL, 0)
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, MusicWidgetProvider::class.java)
        val ids = manager.getAppWidgetIds(component)

        for (id in ids) {
            val views = RemoteViews(context.packageName, R.layout.music_widget)

            views.setTextViewText(
                R.id.audio_level,
                "LIVE  •  $level%"
            )

            for ((index, barId) in BAR_IDS.withIndex()) {
                val wave = (index * 7 + level * (index % 4 + 2)) % 100
                val height = (8 + wave * 0.55f).coerceIn(8f, 64f)

                if (Build.VERSION.SDK_INT >= 31) {
                    views.setViewLayoutHeight(
                        barId,
                        height,
                        0
                    )
                }
            }

            manager.updateAppWidget(id, views)
        }
    }
}
