package com.example.widget

import android.content.Context
import android.content.Intent
import android.icu.text.DateFormat
import android.icu.text.SimpleDateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.ZeroApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.layout.Spacer
import androidx.glance.layout.height
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import androidx.glance.appwidget.updateAll

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as ZeroApplication
        val record = app.repository.record.first()
        
        val days = if (record != null) {
            val diff = System.currentTimeMillis() - record.streakStartDateMillis
            TimeUnit.MILLISECONDS.toDays(diff).toInt()
        } else 0
        
        val lastUpdated = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(Color(0xFF000000))
                        .padding(16.dp)
                        .clickable(actionStartActivity(Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        })),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$days",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "DAYS",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    Text(
                        text = "UPDATED $lastUpdated",
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    )
                }
            }
        }
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}

fun updateStreakWidget(context: Context) {
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
        StreakWidget().updateAll(context)
    }
}
