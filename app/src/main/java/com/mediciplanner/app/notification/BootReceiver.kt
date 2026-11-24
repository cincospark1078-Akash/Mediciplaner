package com.mediciplanner.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mediciplanner.app.R
import com.mediciplanner.app.data.local.MediciDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d("BootReceiver", "Boot completed received. Re-scheduling pending reminders.")

            NotificationHelper.createNotificationChannel(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = MediciDatabase.getDatabase(context)
                    val pendingReminders = db.reminderDao().getAllPendingReminders()
                    val scheduler = ReminderScheduler(context)

                    for (reminder in pendingReminders) {
                        val medicineName = db.prescriptionDao().getMedicineNameByScheduleId(reminder.doseScheduleId)
                            ?: context.getString(R.string.today_medicines_header)

                        scheduler.scheduleReminder(reminder, medicineName)
                    }

                    val activeFollowUps = db.followUpDao().getActiveReminderFollowUps()
                    for (followUp in activeFollowUps) {
                        scheduler.scheduleFollowUpReminder(followUp)
                    }
                    Log.d("BootReceiver", "Re-scheduled ${pendingReminders.size} pending reminders and ${activeFollowUps.size} follow-ups after reboot.")
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error re-scheduling reminders after reboot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
