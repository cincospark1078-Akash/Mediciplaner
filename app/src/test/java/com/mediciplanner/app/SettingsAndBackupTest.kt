package com.mediciplanner.app

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.AppThemeMode
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.backup.BackupManager
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.preferences.ThemePreferences
import com.mediciplanner.app.ui.screens.settings.SettingsScreen
import com.mediciplanner.app.ui.screens.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SettingsAndBackupTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSettingsViewModelStateManagement() = runTest {
        val viewModel = SettingsViewModel()

        val initialState = viewModel.uiState.first()
        assertFalse(initialState.showThemeDialog)
        assertFalse(initialState.showRestoreConfirmDialog)
        assertFalse(initialState.isBackingUp)
        assertFalse(initialState.isRestoring)

        // Show Theme Dialog
        viewModel.showThemeDialog(true)
        assertTrue(viewModel.uiState.first().showThemeDialog)

        viewModel.showThemeDialog(false)
        assertFalse(viewModel.uiState.first().showThemeDialog)

        // Show Restore Confirmation Dialog
        viewModel.showRestoreConfirmDialog(true)
        assertTrue(viewModel.uiState.first().showRestoreConfirmDialog)

        viewModel.showRestoreConfirmDialog(false)
        assertFalse(viewModel.uiState.first().showRestoreConfirmDialog)

        // Progress indicators
        viewModel.setBackingUp(true)
        assertTrue(viewModel.uiState.first().isBackingUp)
        viewModel.setBackingUp(false)

        viewModel.setRestoring(true)
        assertTrue(viewModel.uiState.first().isRestoring)
        viewModel.setRestoring(false)
    }

    @Test
    fun testBackupCreatesZipWithDatabaseAndImages() {
        // Prepare dummy db file
        val dbFile = context.getDatabasePath(MediciDatabase.DATABASE_NAME)
        dbFile.parentFile?.mkdirs()
        dbFile.writeText("SQLite format 3 - Medici Test Database Content")

        // Prepare dummy image file
        val imagesDir = File(context.filesDir, "medicine_images")
        imagesDir.mkdirs()
        val dummyImage = File(imagesDir, "paracetamol_box.jpg")
        dummyImage.writeText("Dummy image JPEG binary data")

        // Perform backup
        val result = BackupManager.backupData(context)
        assertTrue("Backup should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)
        val backupUri = result.getOrThrow()

        // Inspect the generated zip
        val zipFileOnDisk = if (backupUri.scheme == "file") {
            File(backupUri.path!!)
        } else {
            // For content URI, copy to temporary file to inspect
            val tempInspect = File(context.cacheDir, "inspect_backup.zip")
            context.contentResolver.openInputStream(backupUri)?.use { input ->
                tempInspect.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempInspect
        }

        assertTrue("Zip file must exist on disk", zipFileOnDisk.exists())
        val zip = ZipFile(zipFileOnDisk)
        val entries = zip.entries().toList().map { it.name }

        assertTrue("Zip must contain medici.db", entries.contains(BackupManager.BACKUP_DB_ENTRY_NAME))
        assertTrue("Zip must contain medicine_images/paracetamol_box.jpg", entries.any { it.contains("paracetamol_box.jpg") })

        zip.close()
    }

    @Test
    fun testRestoreReplacesDatabaseAndImages() {
        // Create a custom backup archive to restore from
        val restoreSourceZip = File(context.cacheDir, "test_restore_source.zip")
        ZipOutputStream(FileOutputStream(restoreSourceZip)).use { zos ->
            // Add medici.db
            val dbEntry = ZipEntry(BackupManager.BACKUP_DB_ENTRY_NAME)
            zos.putNextEntry(dbEntry)
            zos.write("Restored Database Content Header".toByteArray())
            zos.closeEntry()

            // Add restored image
            val imgEntry = ZipEntry("${BackupManager.IMAGES_DIR_NAME}/restored_aspirin.png")
            zos.putNextEntry(imgEntry)
            zos.write("Restored Image Data".toByteArray())
            zos.closeEntry()
        }

        val zipUri = Uri.fromFile(restoreSourceZip)

        // Perform restore
        val result = BackupManager.restoreData(context, zipUri)
        assertTrue("Restore should succeed: ${result.exceptionOrNull()?.message}", result.isSuccess)

        // Verify target database was replaced
        val targetDb = context.getDatabasePath(MediciDatabase.DATABASE_NAME)
        assertTrue("Target database must exist", targetDb.exists())
        assertEquals("Restored Database Content Header", targetDb.readText())

        // Verify image was restored
        val restoredImage = File(context.filesDir, "${BackupManager.IMAGES_DIR_NAME}/restored_aspirin.png")
        assertTrue("Restored image must exist in medicine_images", restoredImage.exists())
        assertEquals("Restored Image Data", restoredImage.readText())
    }

    @Test
    fun testThemePreferencesPersistence() = kotlinx.coroutines.runBlocking {
        val themePreferences = ThemePreferences(context)

        themePreferences.setThemeMode(AppThemeMode.MALE)
        assertEquals(AppThemeMode.MALE, themePreferences.themeModeFlow.first())

        themePreferences.setThemeMode(AppThemeMode.FEMALE)
        assertEquals(AppThemeMode.FEMALE, themePreferences.themeModeFlow.first())

        themePreferences.setThemeMode(AppThemeMode.NEUTRAL)
        assertEquals(AppThemeMode.NEUTRAL, themePreferences.themeModeFlow.first())
    }

    @Test
    fun testTimeFormatDataStorePersistenceAndViewModel() = kotlinx.coroutines.runBlocking {
        val themePreferences = ThemePreferences(context)
        // Reset to default 12H and verify
        themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_12H)
        assertEquals("12H", themePreferences.timeFormatFlow.first())

        val viewModel = SettingsViewModel(themePreferences)
        assertEquals("12H", viewModel.timeFormat.value)

        // Switch to 24H via ViewModel
        viewModel.updateTimeFormat("24H")
        kotlinx.coroutines.delay(150)
        assertEquals("24H", viewModel.timeFormat.value)
        assertEquals("24H", themePreferences.timeFormatFlow.first())

        // Switch back to 12H via ViewModel
        viewModel.updateTimeFormat("12H")
        kotlinx.coroutines.delay(150)
        assertEquals("12H", viewModel.timeFormat.value)
        assertEquals("12H", themePreferences.timeFormatFlow.first())
    }

    @Test
    fun testSettingsScreenTimeFormatToggleUpdatesDataStore() {
        val themePreferences = ThemePreferences(context)
        kotlinx.coroutines.runBlocking {
            themePreferences.setTimeFormat(ThemePreferences.TIME_FORMAT_12H)
        }
        val viewModel = SettingsViewModel(themePreferences)

        composeTestRule.setContent {
            MediciTheme {
                SettingsScreen(
                    currentTheme = AppThemeMode.NEUTRAL,
                    onThemeSelected = {},
                    onSaveThemePermanently = {},
                    onNavigateBack = {},
                    settingsViewModel = viewModel
                )
            }
        }

        composeTestRule.onNodeWithTag("settings_time_format_card")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("সময়ের ফরম্যাট").assertIsDisplayed()
        composeTestRule.onNodeWithText("১২ ঘণ্টা (AM/PM)").assertIsDisplayed()
        composeTestRule.onNodeWithText("২৪ ঘণ্টা").assertIsDisplayed()

        // Initially 12H is selected
        composeTestRule.onNodeWithTag("time_format_chip_12h").assertIsSelected()

        // Click 24H option
        composeTestRule.onNodeWithTag("time_format_chip_24h").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("time_format_chip_24h").assertIsSelected()
        assertEquals("24H", viewModel.timeFormat.value)
        val persisted = kotlinx.coroutines.runBlocking {
            kotlinx.coroutines.delay(150)
            themePreferences.timeFormatFlow.first()
        }
        assertEquals("24H", persisted)
    }
}
