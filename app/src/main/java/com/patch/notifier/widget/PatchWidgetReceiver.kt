package com.patch.notifier.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.patch.notifier.MainActivity
import com.patch.notifier.R
import com.patch.notifier.data.PatchDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PatchWidgetReceiver : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = loadState(context)
                for (widgetId in appWidgetIds) {
                    updateWidget(context, appWidgetManager, widgetId, state)
                }
            } catch (e: Exception) {
                Log.e("PatchWidget", "Failed to update widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun updateAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                android.content.ComponentName(context, PatchWidgetReceiver::class.java),
            )
            if (widgetIds.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val state = loadState(context)
                    for (widgetId in widgetIds) {
                        updateWidget(context, appWidgetManager, widgetId, state)
                    }
                } catch (e: Exception) {
                    Log.e("PatchWidget", "Failed to update widgets", e)
                }
            }
        }

        private suspend fun loadState(context: Context): PatchWidgetState {
            return try {
                val dao = PatchDatabase.getInstance(context).patchDao()
                val patches = dao.getActivePatchesByDueDate()
                val now = System.currentTimeMillis()
                val earliest = patches.firstOrNull()?.dueAt
                computeWidgetState(earliest, patches.size, now)
            } catch (_: Exception) {
                PatchWidgetState(null, 0)
            }
        }

        private fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            state: PatchWidgetState,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_patch)

            views.setTextViewText(R.id.widget_days, state.displayText)
            views.setTextViewText(R.id.widget_subtitle, state.subtitleText)

            val accentColor = when (state.urgency) {
                WidgetUrgency.GOOD -> 0xFF3D5AFE.toInt()
                WidgetUrgency.SOON -> 0xFFF59E0B.toInt()
                WidgetUrgency.OVERDUE -> 0xFFEF4444.toInt()
                WidgetUrgency.NONE -> 0xFF6C6C8A.toInt()
            }
            views.setTextColor(R.id.widget_days, accentColor)

            // Tap opens app
            val launchIntent = Intent(context, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                context, 0, launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}
