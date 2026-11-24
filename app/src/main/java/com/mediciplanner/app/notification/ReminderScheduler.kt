package com.mediciplanner.app.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.mediciplanner.app.data.local.entity.ReminderEntity
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedules an exact alarm that survives Doze / Sleep mode using setExactAndAllowWhileIdle.
     */
    fun scheduleReminder(reminder: ReminderEntity, medicineName: String) {
        val triggerTimeMillis = parseScheduledAtToMillis(reminder.scheduledAt) ?: return
        val now = System.currentTimeMillis()

        // Do not schedule alarms for timestamps that are more than 1 minute in the past
        if (triggerTimeMillis < now - 60_000L) {
            Log.d("ReminderScheduler", "Skipping past reminder ${reminder.id} at ${reminder.scheduledAt}")
            return
        }

        val triggerTime = if (triggerTimeMillis <= now) now + 1_000L else triggerTimeMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderConstants.ACTION_FIRE_REMINDER
            putExtra(ReminderConstants.EXTRA_REMINDER_ID, reminder.id)
            putExtra(ReminderConstants.EXTRA_MEDICINE_NAME, medicineName)
            putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, reminder.scheduledAt)
            putExtra("notification_type", "medicine")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled reminder for ${reminder.id} at $triggerTime ($medicineName)")
        } catch (e: SecurityException) {
            Log.w("ReminderScheduler", "SecurityException scheduling exact alarm, falling back to inexact alarm: ${e.message}")
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } catch (fallbackEx: Exception) {
                Log.e("ReminderScheduler", "Failed to schedule reminder alarm fallback", fallbackEx)
            }
        }
    }

    /**
     * Schedules a follow-up reminder using the entity's custom reminderTime on the follow-up date.
     * Cancels existing reminder if follow-up is already completed.
     */
    fun scheduleFollowUpReminder(followUp: com.mediciplanner.app.data.local.entity.FollowUpEntity) {
        if (followUp.isCompleted) {
            cancelFollowUpReminder(followUp.id)
            return
        }

        val dateClean = followUp.date.trim().take(10)
        val timeClean = when {
            followUp.reminderTime.isBlank() -> "09:00:00"
            followUp.reminderTime.length == 5 -> "${followUp.reminderTime}:00"
            followUp.reminderTime.length >= 8 -> followUp.reminderTime.take(8)
            else -> "09:00:00"
        }
        val scheduledAt = "${dateClean}T$timeClean"
        val triggerTimeMillis = parseScheduledAtToMillis(scheduledAt) ?: return
        val now = System.currentTimeMillis()

        if (triggerTimeMillis < now - 60_000L) {
            Log.d("ReminderScheduler", "Skipping past follow-up reminder ${followUp.id} at $scheduledAt")
            return
        }

        val triggerTime = if (triggerTimeMillis <= now) now + 1_000L else triggerTimeMillis

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderConstants.ACTION_FIRE_REMINDER
            putExtra(ReminderConstants.EXTRA_REMINDER_ID, followUp.id)
            putExtra(ReminderConstants.EXTRA_MEDICINE_NAME, followUp.title)
            putExtra(ReminderConstants.EXTRA_SCHEDULED_AT, scheduledAt)
            putExtra("notification_type", "followup")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            followUp.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                } catch (secEx: SecurityException) {
                    Log.w("ReminderScheduler", "SecurityException scheduling exact follow-up alarm: ${secEx.message}, falling back to inexact")
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
            Log.d("ReminderScheduler", "Scheduled follow-up alarm for ${followUp.id} at $timeClean ($triggerTime)")
        } catch (e: Exception) {
            Log.w("ReminderScheduler", "Could not schedule follow-up alarm: ${e.message}")
        }
    }

    /**
     * Cancels an existing follow-up alarm.
     */
    fun cancelFollowUpReminder(followUpId: String) {
        cancelReminder(followUpId)
    }

    /**
     * Cancels an existing scheduled alarm PendingIntent for the given reminderId.
     */
    fun cancelReminder(reminderId: String) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderConstants.ACTION_FIRE_REMINDER
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d("ReminderScheduler", "Cancelled reminder alarm for $reminderId")
        }
    }

    fun notifyLowStock(medicineId: String, medicineName: String) {
        NotificationHelper.showLowStockNotification(
            context = context,
            medicineId = medicineId,
            medicineName = medicineName
        )
    }

    private fun parseScheduledAtToMillis(scheduledAt: String): Long? {
        val clean = scheduledAt.trim().replace(" ", "T")
        return try {
            val ldt = when {
                clean.length == 16 -> LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"))
                clean.length == 19 -> LocalDateTime.parse(clean, DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                else -> LocalDateTime.parse(clean, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }
            ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            try {
                val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                format.parse(clean)?.time
            } catch (_: Exception) {
                null
            }
        }
    }
}
