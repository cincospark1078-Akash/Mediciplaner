package com.mediciplanner.app.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mediciplanner.app.MainActivity
import com.mediciplanner.app.MediciApplication
import com.mediciplanner.app.R
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.core.util.formatTime
import kotlinx.coroutines.flow.first

object NotificationHelper {
    const val CHANNEL_ID = "medici_reminder_channel_v2"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = context.getString(R.string.notification_channel_name)
            val channelDescription = context.getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_HIGH

            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    suspend fun is24HourPreferred(context: Context): Boolean {
        val prefs = (context.applicationContext as? MediciApplication)?.themePreferences
            ?: ThemePreferences(context.applicationContext)
        return runCatching {
            prefs.timeFormatFlow.first() == ThemePreferences.TIME_FORMAT_24H
        }.getOrDefault(false)
    }

    fun formatNotificationTime(scheduledAt: String?, is24Hour: Boolean): String {
        if (scheduledAt.isNullOrBlank()) return ""
        return formatTime(scheduledAt, is24Hour)
    }

    fun buildReminderNotificationText(
        context: Context,
        type: String,
        itemName: String,
        scheduledAt: String?,
        is24Hour: Boolean
    ): Pair<String, String> {
        val formattedTime = formatNotificationTime(scheduledAt, is24Hour)
        val title = if (type == "followup") {
            context.getString(R.string.notification_followup_title)
        } else {
            context.getString(R.string.notification_reminder_title)
        }
        val body = if (formattedTime.isNotBlank()) {
            if (type == "followup") {
                context.getString(R.string.notification_followup_body_with_time, itemName, formattedTime)
            } else {
                context.getString(R.string.notification_reminder_body_with_time, itemName, formattedTime)
            }
        } else {
            if (type == "followup") {
                context.getString(R.string.notification_followup_body, itemName)
            } else {
                context.getString(R.string.notification_reminder_body, itemName)
            }
        }
        return title to body
    }

    fun showLowStockNotification(
        context: Context,
        medicineId: String,
        medicineName: String
    ) {
        createNotificationChannel(context)
        val notificationId = "low_stock_$medicineId".hashCode()
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = context.getString(R.string.notification_low_stock_title)
        val body = context.getString(R.string.notification_low_stock_body, medicineName)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            Log.w("NotificationHelper", "Skipped low stock notification due to missing permission", e)
        }
    }
}
