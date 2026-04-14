package com.patch.notifier.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.patch.notifier.MainActivity
import com.patch.notifier.data.PatchDatabase

class PatchWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val state = loadState(context)

        provideContent {
            PatchWidgetContent(state, context)
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
}

@Composable
private fun PatchWidgetContent(state: PatchWidgetState, context: Context) {
    val bgColor = when (state.urgency) {
        WidgetUrgency.GOOD -> ColorProvider(android.graphics.Color.parseColor("#1A1A2E"))
        WidgetUrgency.SOON -> ColorProvider(android.graphics.Color.parseColor("#2E2A1A"))
        WidgetUrgency.OVERDUE -> ColorProvider(android.graphics.Color.parseColor("#2E1A1A"))
        WidgetUrgency.NONE -> ColorProvider(android.graphics.Color.parseColor("#1A1A2E"))
    }

    val accentColor = when (state.urgency) {
        WidgetUrgency.GOOD -> ColorProvider(android.graphics.Color.parseColor("#3D5AFE"))
        WidgetUrgency.SOON -> ColorProvider(android.graphics.Color.parseColor("#F59E0B"))
        WidgetUrgency.OVERDUE -> ColorProvider(android.graphics.Color.parseColor("#EF4444"))
        WidgetUrgency.NONE -> ColorProvider(android.graphics.Color.parseColor("#6C6C8A"))
    }

    val subtitleColor = ColorProvider(android.graphics.Color.parseColor("#8B8BA7"))

    val launchIntent = Intent(context, MainActivity::class.java)

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(bgColor)
            .padding(4.dp)
            .clickable(actionStartActivity(launchIntent)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = state.displayText,
            style = TextStyle(
                color = accentColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            ),
        )
        Text(
            text = state.subtitleText,
            style = TextStyle(
                color = subtitleColor,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}
