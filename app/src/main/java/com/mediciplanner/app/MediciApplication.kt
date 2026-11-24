package com.mediciplanner.app

import android.app.Application
import com.mediciplanner.app.core.util.OcrHelper
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.notification.NotificationHelper
import com.mediciplanner.app.notification.ReminderScheduler

class MediciApplication : Application() {

    lateinit var themePreferences: ThemePreferences
        private set

    val database: MediciDatabase by lazy {
        MediciDatabase.getDatabase(this)
    }

    val prescriptionRepository: PrescriptionRepository by lazy {
        PrescriptionRepository(database.prescriptionDao())
    }

    val reminderRepository: ReminderRepository by lazy {
        ReminderRepository(database.reminderDao())
    }

    val followUpRepository: FollowUpRepository by lazy {
        FollowUpRepository(database.followUpDao())
    }

    val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(this)
    }

    val ocrHelper: OcrHelper by lazy {
        OcrHelper(this)
    }

    override fun onCreate() {
        super.onCreate()
        val locale = java.util.Locale("bn")
        java.util.Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        themePreferences = ThemePreferences(this)
        NotificationHelper.createNotificationChannel(this)
    }
}
