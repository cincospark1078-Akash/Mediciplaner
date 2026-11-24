package com.mediciplanner.app.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.mediciplanner.app.MainActivity
import com.mediciplanner.app.R
import com.mediciplanner.app.core.data.preferences.ThemePreferences
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.ReminderStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("ReminderReceiver", "onReceive action=$action")

        when (action) {
            ReminderConstants.ACTION_MARK_TAKEN -> {
                handleMarkTakenAction(context, intent)
            }
            ReminderConstants.ACTION_FIRE_REMINDER, null -> {
                handleFireReminder(context, intent)
            }
        }
    }

    private fun handleFireReminder(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderConstants.EXTRA_REMINDER_ID) ?: return
        val medicineName = intent.getStringExtra(ReminderConstants.EXTRA_MEDICINE_NAME) ?: context.getString(R.string.today_medicines_header)
        val scheduledAt = intent.getStringExtra(ReminderConstants.EXTRA_SCHEDULED_AT)
        val notificationId = reminderId.hashCode()

        val type = intent.getStringExtra("notification_type") ?: "medicine"

        // Ensure notification channel is initialized
        NotificationHelper.createNotificationChannel(context)

        // 1. Content Intent: When user taps the main body of the notification, open directly to Home Screen
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            this.action = ReminderConstants.ACTION_NOTIFICATION_TAP
            putExtra(ReminderConstants.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra("notification_type", type)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 2. Action Button Intent: "গ্রহণ করেছি" (Mark as Taken) directly from the notification (for medicine)
        val markTakenIntent = Intent(context, ReminderReceiver::class.java).apply {
            this.action = ReminderConstants.ACTION_MARK_TAKEN
            putExtra(ReminderConstants.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderConstants.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra("notification_type", type)
        }

        val markTakenPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 1,
            markTakenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markTakenLabel = context.getString(R.string.action_mark_taken)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = ThemePreferences(context)
                val isEnabled = if (type == "medicine") {
                    prefs.medicineRemindersEnabledFlow.first()
                } else {
                    prefs.followupRemindersEnabledFlow.first()
                }
                if (!isEnabled) {
                    Log.d("ReminderReceiver", "Skipped $type notification because user disabled it in settings")
                    return@launch
                }

                val soundEnabled = prefs.notificationSoundEnabledFlow.first()
                val is24Hour = NotificationHelper.is24HourPreferred(context)
                val (notificationTitle, notificationBody) = NotificationHelper.buildReminderNotificationText(
                    context = context,
                    type = type,
                    itemName = medicineName,
                    scheduledAt = scheduledAt,
                    is24Hour = is24Hour
                )

                val builder = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(notificationTitle)
                    .setContentText(notificationBody)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(notificationBody))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setDefaults(if (soundEnabled) NotificationCompat.DEFAULT_ALL else NotificationCompat.DEFAULT_VIBRATE)
                    .setSilent(!soundEnabled)
                    .setAutoCancel(true)
                    .setColor(0xFF8BB8D6.toInt())
                    .setContentIntent(contentPendingIntent)

                if (type == "medicine") {
                    builder.addAction(
                        R.drawable.ic_launcher_foreground,
                        markTakenLabel,
                        markTakenPendingIntent
                    )
                }

                val notificationManager = NotificationManagerCompat.from(context)
                try {
                    notificationManager.notify(notificationId, builder.build())
                    Log.d("ReminderReceiver", "Displayed reminder notification $notificationId for $medicineName")
                } catch (e: SecurityException) {
                    Log.e("ReminderReceiver", "SecurityException posting notification", e)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun handleMarkTakenAction(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(ReminderConstants.EXTRA_REMINDER_ID) ?: return
        val notificationId = intent.getIntExtra(ReminderConstants.EXTRA_NOTIFICATION_ID, reminderId.hashCode())

        // 1. Dismiss notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId)

        // 2. Cancel scheduled alarm
        val scheduler = ReminderScheduler(context)
        scheduler.cancelReminder(reminderId)

        // 3. Update Room Database status to TAKEN and takenAt = currentTime
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MediciDatabase.getDatabase(context)
                val existing = db.reminderDao().getReminderById(reminderId)
                val nowIso = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                db.reminderDao().updateReminderStatus(reminderId, ReminderStatus.TAKEN, nowIso)
                if (existing != null && existing.status != ReminderStatus.TAKEN) {
                    val med = db.prescriptionDao().getMedicineByScheduleId(existing.doseScheduleId)
                    if (med != null) {
                        val updatedMed = db.prescriptionDao().decrementStockIfAvailable(med.id)
                        if (updatedMed != null && updatedMed.stockCount == 5) {
                            NotificationHelper.showLowStockNotification(context, updatedMed.id, updatedMed.name)
                        }
                    }
                }
                Log.d("ReminderReceiver", "Marked reminder $reminderId as TAKEN in database")
            } catch (e: Exception) {
                Log.e("ReminderReceiver", "Error updating reminder status in database", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
