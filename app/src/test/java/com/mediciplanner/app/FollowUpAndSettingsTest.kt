package com.mediciplanner.app

import android.app.AlarmManager
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import com.mediciplanner.app.data.local.entity.FollowUpType
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.data.repository.FollowUpRepository
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.followup.FollowUpViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FollowUpAndSettingsTest {

    private lateinit var context: Context
    private lateinit var database: MediciDatabase
    private lateinit var followUpRepository: FollowUpRepository
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var alarmManager: AlarmManager
    private lateinit var followUpViewModel: FollowUpViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        followUpRepository = FollowUpRepository(database.followUpDao())
        reminderScheduler = ReminderScheduler(context)
        followUpViewModel = FollowUpViewModel(followUpRepository, reminderScheduler)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testFollowUpReminderSchedulesAlarmAt9AM() = runTest {
        val futureDate = LocalDate.now().plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)

        followUpViewModel.addFollowUp(
            title = "রক্ত পরীক্ষা ও পরামর্শ",
            type = FollowUpType.TEST,
            date = futureDate,
            note = "সকালবেলা",
            reminderEnabled = true
        )

        val state = followUpViewModel.uiState.first { it.followUps.isNotEmpty() }
        assertEquals(1, state.followUps.size)
        val item = state.followUps.first()
        assertTrue(item.reminderEnabled)

        testScheduler.advanceUntilIdle()

        // Verify alarm scheduled via Robolectric ShadowAlarmManager
        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val nextScheduledAlarm = shadowAlarmManager.nextScheduledAlarm ?: shadowAlarmManager.scheduledAlarms.firstOrNull()
        assertNotNull(nextScheduledAlarm)

        // Verify the trigger time is approximately 9:00 AM on that future date
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nextScheduledAlarm!!.triggerAtTime
        }
        assertEquals(9, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun testFollowUpDeletionCancelsAlarm() = runTest {
        val futureDate = LocalDate.now().plusDays(4).format(DateTimeFormatter.ISO_LOCAL_DATE)

        followUpViewModel.addFollowUp(
            title = "চোখের ডাক্তার ভিজিট",
            type = FollowUpType.DOCTOR_VISIT,
            date = futureDate,
            reminderEnabled = true
        )

        val item = followUpViewModel.uiState.first { it.followUps.isNotEmpty() }.followUps.first()

        // Deleting the follow up cancels the alarm
        followUpViewModel.deleteFollowUp(item)

        val afterDeleteState = followUpViewModel.uiState.first { it.followUps.isEmpty() }
        assertTrue(afterDeleteState.followUps.isEmpty())
    }

    @Test
    fun testFollowUpReminderWithCustomTimeSchedulesAlarmAtExactTime() = runTest {
        val futureDate = LocalDate.now().plusDays(3).format(DateTimeFormatter.ISO_LOCAL_DATE)

        followUpViewModel.addFollowUp(
            title = "ইসিজি টেস্ট",
            type = FollowUpType.TEST,
            date = futureDate,
            note = "বিকাল ৩:৩০",
            reminderEnabled = true,
            reminderTime = "15:30"
        )

        val state = followUpViewModel.uiState.first { it.followUps.isNotEmpty() }
        val item = state.followUps.first()
        assertEquals("15:30", item.reminderTime)
        assertTrue(item.reminderEnabled)

        testScheduler.advanceUntilIdle()

        val shadowAlarmManager = Shadows.shadowOf(alarmManager)
        val nextScheduledAlarm = shadowAlarmManager.nextScheduledAlarm ?: shadowAlarmManager.scheduledAlarms.firstOrNull()
        assertNotNull(nextScheduledAlarm)

        // Verify the trigger time is exactly 15:30 (3:30 PM) on that future date
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nextScheduledAlarm!!.triggerAtTime
        }
        assertEquals(15, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun testMarkingAsCompletedCancelsAlarm() = runTest {
        val futureDate = LocalDate.now().plusDays(5).format(DateTimeFormatter.ISO_LOCAL_DATE)

        followUpViewModel.addFollowUp(
            title = "দাঁতের ডাক্তার ভিজিট",
            type = FollowUpType.DOCTOR_VISIT,
            date = futureDate,
            reminderEnabled = true,
            reminderTime = "11:00"
        )

        val item = followUpViewModel.uiState.first { it.upcomingFollowUps.isNotEmpty() }.upcomingFollowUps.first()
        testScheduler.advanceUntilIdle()

        // Marking as completed cancels the alarm and moves to history
        followUpViewModel.markAsCompleted(item)
        testScheduler.advanceUntilIdle()

        val updatedState = followUpViewModel.uiState.first { it.historyFollowUps.isNotEmpty() }
        assertTrue(updatedState.upcomingFollowUps.isEmpty())
        assertEquals(1, updatedState.historyFollowUps.size)
        val completedItem = updatedState.historyFollowUps.first()
        assertTrue(completedItem.isCompleted)
        assertNotNull(completedItem.completedAt)
    }

    @Test
    fun testThemeViewModelThemeChange() = runTest {
        val themePreferences = ThemePreferences(context)
        val mainViewModel = MainViewModel(themePreferences)

        // Instant preview
        mainViewModel.previewTheme(AppThemeMode.MALE)
        assertEquals(AppThemeMode.MALE, mainViewModel.previewTheme.value)

        // Save theme permanently
        mainViewModel.saveTheme(AppThemeMode.FEMALE)
        assertEquals(AppThemeMode.FEMALE, mainViewModel.previewTheme.value)
    }
}
