package com.mediciplanner.app

import android.app.AlarmManager
import android.content.Context
import android.os.Looper
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.entity.ReminderEntity
import com.mediciplanner.app.data.local.entity.ReminderStatus
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.data.repository.ReminderRepository
import com.mediciplanner.app.data.util.ReminderIdGenerator
import com.mediciplanner.app.notification.ReminderConstants
import com.mediciplanner.app.notification.ReminderScheduler
import com.mediciplanner.app.ui.screens.prescription.AddPrescriptionListScreen
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.PrescriptionListScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowToast
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class AddPrescriptionListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var database: MediciDatabase
    private lateinit var prescriptionDao: PrescriptionDao
    private lateinit var prescriptionRepository: PrescriptionRepository
    private lateinit var reminderRepository: ReminderRepository
    private lateinit var reminderScheduler: ReminderScheduler
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var viewModel: PrescriptionCarouselViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        prescriptionDao = database.prescriptionDao()
        prescriptionRepository = PrescriptionRepository(prescriptionDao)
        reminderRepository = ReminderRepository(database.reminderDao())
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = Shadows.shadowOf(alarmManager)
        reminderScheduler = ReminderScheduler(context)
        viewModel = PrescriptionCarouselViewModel(
            prescriptionRepository = prescriptionRepository,
            reminderRepository = reminderRepository,
            reminderScheduler = reminderScheduler
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun testNewListScreenShowsTopBarBaseInfoAndFixedBottomButtons() {
        var dismissed = false

        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Top Bar
        composeTestRule.onNodeWithTag("add_prescription_list_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_prescription_close_button").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_prescription_title")
            .assertIsDisplayed()
            .assertTextEquals("প্রেস্ক্রিপশন তৈরি করুন")
        composeTestRule.onNodeWithTag("add_prescription_edit_badge").assertDoesNotExist()

        // Base Info Section
        composeTestRule.onNodeWithTag("add_prescription_base_info_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("prescription_date_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("doctor_name_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_input").assertIsDisplayed()

        // Fixed Bottom Buttons
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button")
            .assertIsDisplayed()
            .assertTextContains("নতুন ওষুধ যোগ করুন")
        composeTestRule.onNodeWithTag("add_prescription_save_button")
            .assertIsDisplayed()
            .assertTextContains("সংরক্ষণ করুন")

        // Verify fixed bottom buttons stay below scrollable lazy column
        val listBounds = composeTestRule.onNodeWithTag("add_prescription_lazy_column").fetchSemanticsNode().boundsInRoot
        val addBtnBounds = composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").fetchSemanticsNode().boundsInRoot
        val saveBtnBounds = composeTestRule.onNodeWithTag("add_prescription_save_button").fetchSemanticsNode().boundsInRoot
        assertTrue(addBtnBounds.top >= listBounds.bottom - 1f)
        assertTrue(saveBtnBounds.top > addBtnBounds.top)

        // Base info inputs update draft
        composeTestRule.onNodeWithTag("doctor_name_input").performTextInput("ডা. আনিস")
        composeTestRule.onNodeWithTag("location_input").performTextInput("ঢাকা মেডিকেল")
        composeTestRule.waitForIdle()
        assertEquals("ডা. আনিস", viewModel.draftPrescription.value.doctorName)
        assertEquals("ঢাকা মেডিকেল", viewModel.draftPrescription.value.location)

        // Saving without date displays required error
        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("error_date_required").assertIsDisplayed()

        // Close button dismisses screen
        composeTestRule.onNodeWithTag("add_prescription_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun testExpandableMedicineCardAddToggleExpandAndDelete() {
        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Initially empty hint shown
        composeTestRule.onNodeWithTag("add_prescription_empty_medicines_card").assertIsDisplayed()

        // Add first medicine -> Card "ওষুধ ১" added & expanded
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_form_card_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_card_title_0", useUnmergedTree = true)
            .assertIsDisplayed()
            .assertTextEquals("ওষুধ ১")
        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_name_input").assertIsDisplayed()

        // Collapse first card
        composeTestRule.onNodeWithTag("medicine_form_card_toggle_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertDoesNotExist()

        // Expand again
        composeTestRule.onNodeWithTag("medicine_form_card_toggle_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertIsDisplayed()

        // Add second medicine -> "ওষুধ ২"
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_card_title_1", useUnmergedTree = true)
            .assertTextEquals("ওষুধ ২")
        assertEquals(2, viewModel.draftPrescription.value.medicines.size)

        // Scroll back to first medicine in LazyColumn and delete it -> remaining shifts to "ওষুধ ১"
        composeTestRule.onNodeWithTag("add_prescription_lazy_column").performScrollToIndex(2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_delete_0").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, viewModel.draftPrescription.value.medicines.size)
        composeTestRule.onNodeWithTag("medicine_form_card_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_card_1").assertDoesNotExist()
    }

    @Test
    fun testEditModeLoadsPrescriptionAndSelectedMedicine() = runTest {
        val presId = UUID.randomUUID().toString()
        val prescription = PrescriptionEntity(
            id = presId,
            parentId = presId,
            versionNumber = 1,
            isLatest = true,
            date = "2026-11-21",
            doctorName = "ডা. হাসান",
            location = "স্কয়ার হাসপাতাল",
            createdAt = "2026-11-21T09:00:00"
        )
        val med1 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Maxpro",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 14,
            foodRule = FoodRule.BEFORE,
            createdAt = "2026-11-21T09:00:00"
        )
        val med2 = MedicineEntity(
            id = UUID.randomUUID().toString(),
            prescriptionId = presId,
            name = "Napa",
            type = MedicineType.TABLET,
            strength = 500.0,
            strengthUnit = "mg",
            durationDays = 7,
            foodRule = FoodRule.AFTER,
            createdAt = "2026-11-21T09:05:00"
        )
        val sched1 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med1.id,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        val sched2 = DoseScheduleEntity(
            id = UUID.randomUUID().toString(),
            medicineId = med2.id,
            slot = DoseSlot.NIGHT,
            time = "22:00",
            doseAmount = 1.0,
            doseUnit = "টি"
        )
        prescriptionRepository.saveFullPrescription(
            prescription = prescription,
            medicines = listOf(med1, med2),
            schedules = listOf(sched1, sched2)
        )

        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    medicineId = med2.id,
                    prescriptionId = presId,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_prescription_edit_badge").assertIsDisplayed()
        composeTestRule.onNodeWithTag("doctor_name_input").assertTextContains("ডা. হাসান")
        composeTestRule.onNodeWithTag("location_input").assertTextContains("স্কয়ার হাসপাতাল")
        composeTestRule.onNodeWithTag("prescription_date_input").assertTextContains("২১ নভেম্বর, ২০২৬")

        // Existing medicines shown in expandable cards, and selected medicine (index 1) is auto-expanded
        composeTestRule.onNodeWithTag("medicine_form_card_0").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_card_subtitle_0", useUnmergedTree = true)
            .assertTextEquals("Maxpro")
        composeTestRule.onNodeWithTag("medicine_form_card_1").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_card_subtitle_1", useUnmergedTree = true)
            .assertTextEquals("Napa")
        composeTestRule.onNodeWithTag("medicine_form_card_content_1").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun testPrescriptionListScreenFabNavigatesToAddPrescriptionListScreen() {
        composeTestRule.setContent {
            MediciTheme {
                PrescriptionListScreen()
            }
        }
        composeTestRule.waitForIdle()

        // Tap FAB on PrescriptionListScreen
        composeTestRule.onNodeWithTag("add_prescription_fab").performClick()
        composeTestRule.waitForIdle()

        // Navigates to AddPrescriptionListScreen instead of old PrescriptionCarouselScreen
        composeTestRule.onNodeWithTag("add_prescription_list_screen").assertIsDisplayed()
        composeTestRule.onNodeWithTag("carousel_card_1").assertDoesNotExist()

        // Tapping Close returns to PrescriptionListScreen
        composeTestRule.onNodeWithTag("add_prescription_close_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("prescription_list_screen").assertIsDisplayed()
    }

    @Test
    fun testMedicineFormCardAllSectionsTypeUnitSyncAndSlotInputs() {
        viewModel.updateDate("2026-11-25")

        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Add one medicine card
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()

        // Verify all required section headers
        composeTestRule.onNodeWithTag("medicine_form_0_header_name").performScrollTo().assertTextEquals("ওষুধের নাম")
        composeTestRule.onNodeWithTag("medicine_form_0_header_type").performScrollTo().assertTextEquals("ধরন")
        composeTestRule.onNodeWithTag("medicine_form_0_header_strength").performScrollTo().assertTextEquals("মাত্রা")
        composeTestRule.onNodeWithTag("medicine_form_0_header_schedule_dose").performScrollTo().assertTextEquals("সময়সূচি ও ডোজ")
        composeTestRule.onNodeWithTag("medicine_form_0_header_duration_stock").performScrollTo().assertTextEquals("মেয়াদ ও স্টক")
        composeTestRule.onNodeWithTag("medicine_form_0_header_note").performScrollTo().assertTextEquals("নোট")

        // 1. Name input
        composeTestRule.onNodeWithTag("medicine_form_0_name_input").performScrollTo().performTextInput("ন্যাপা এক্সটেন্ড")
        composeTestRule.waitForIdle()
        assertEquals("ন্যাপা এক্সটেন্ড", viewModel.draftPrescription.value.medicines[0].name)

        // 2. Type Chips + Unit sync: pick SYRUP -> select mg/5mL -> switch to TABLET -> invalid unit cleared
        composeTestRule.onNodeWithTag("medicine_form_0_type_chip_syrup").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(MedicineType.SYRUP, viewModel.draftPrescription.value.medicines[0].type)

        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_item_mg/5mL").performClick()
        composeTestRule.waitForIdle()
        assertEquals("mg/5mL", viewModel.draftPrescription.value.medicines[0].strengthUnit)

        composeTestRule.onNodeWithTag("medicine_form_0_type_chip_tablet").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(MedicineType.TABLET, viewModel.draftPrescription.value.medicines[0].type)
        assertEquals("", viewModel.draftPrescription.value.medicines[0].strengthUnit)

        // 3. Strength + Unit for Tablet
        composeTestRule.onNodeWithTag("medicine_form_0_strength_input").performScrollTo().performTextInput("500")
        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_item_mg").performClick()
        composeTestRule.waitForIdle()
        assertEquals("500", viewModel.draftPrescription.value.medicines[0].strength)
        assertEquals("mg", viewModel.draftPrescription.value.medicines[0].strengthUnit)

        // 4. Slot Chips (MORNING) -> Time Picker + Decimal Dose + Dose Unit Dropdown + Food Rule Chips
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_morning").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_section_morning").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_time_morning").performScrollTo().assertIsDisplayed()

        // Fractional / decimal dose amount
        composeTestRule.onNodeWithTag("medicine_form_0_dose_amount_morning").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("medicine_form_0_dose_amount_morning").performTextInput("1/2")
        composeTestRule.waitForIdle()
        assertEquals("1/2", viewModel.draftPrescription.value.medicines[0].schedules[0].doseAmount)
        assertEquals(0.5, viewModel.draftPrescription.value.medicines[0].schedules[0].parsedDoseAmount!!, 1e-6)

        // Dose Unit Dropdown
        composeTestRule.onNodeWithTag("medicine_form_0_dose_unit_morning").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_dose_unit_item_morning_টি").performClick()
        composeTestRule.waitForIdle()
        assertEquals("টি", viewModel.draftPrescription.value.medicines[0].schedules[0].doseUnit)

        // Food Rule Chips
        composeTestRule.onNodeWithTag("medicine_form_0_food_rule_morning_empty").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_food_rule_morning_empty").assertIsSelected()
        assertEquals(FoodRule.EMPTY, viewModel.draftPrescription.value.medicines[0].schedules[0].foodRule)

        // 5. Interval Chips + Custom Interval Dialog
        composeTestRule.onNodeWithTag("medicine_form_0_interval_chip_2").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertEquals(2, viewModel.draftPrescription.value.medicines[0].intervalDays)

        composeTestRule.onNodeWithTag("medicine_form_0_interval_chip_custom").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_custom_interval_input").performTextInput("5")
        composeTestRule.onNodeWithTag("medicine_form_0_custom_interval_confirm").performClick()
        composeTestRule.waitForIdle()
        assertEquals(5, viewModel.draftPrescription.value.medicines[0].intervalDays)
        composeTestRule.onNodeWithTag("medicine_form_0_interval_chip_custom").assertIsSelected()

        // 6 & 7. Duration & Stock
        composeTestRule.onNodeWithTag("medicine_form_0_duration_input").performScrollTo().performTextInput("15")
        composeTestRule.onNodeWithTag("medicine_form_0_stock_input").performScrollTo().performTextInput("24")
        composeTestRule.waitForIdle()
        assertEquals("15", viewModel.draftPrescription.value.medicines[0].durationDays)
        assertEquals(24, viewModel.draftPrescription.value.medicines[0].stockCount)

        // 8. Note
        composeTestRule.onNodeWithTag("medicine_form_0_note_input").performScrollTo().performTextInput("গ্যাসের সমস্যার জন্য")
        composeTestRule.waitForIdle()
        assertEquals("গ্যাসের সমস্যার জন্য", viewModel.draftPrescription.value.medicines[0].note)
    }

    @Test
    fun testAddPrescriptionListScreenEndToEndSavePersistsAllFieldsToRoom() = runTest {
        viewModel.updateDate("2026-11-25")
        var savedAndDismissed = false

        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = { savedAndDismissed = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("doctor_name_input").performTextInput("ডা. করিম")
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_form_0_name_input").performScrollTo().performTextInput("Seclo")
        composeTestRule.onNodeWithTag("medicine_form_0_type_chip_capsule").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_strength_input").performScrollTo().performTextInput("20")
        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_dropdown").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_strength_unit_item_mg").performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_morning").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_dose_amount_morning").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("medicine_form_0_dose_amount_morning").performTextInput("0.5")
        composeTestRule.onNodeWithTag("medicine_form_0_food_rule_morning_before").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_interval_chip_1").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_duration_input").performScrollTo().performTextInput("14")
        composeTestRule.onNodeWithTag("medicine_form_0_stock_input").performScrollTo().performTextInput("30")
        composeTestRule.onNodeWithTag("medicine_form_0_note_input").performScrollTo().performTextInput("সকালে খালি পেটে")
        composeTestRule.waitForIdle()

        // Tap fixed Save button and wait for async Room persistence
        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            savedAndDismissed || viewModel.saveError.value != null || viewModel.errorMessage.value != null
        }

        assertTrue(
            "Save should succeed and dismiss screen. Error: ${viewModel.errorMessage.value ?: viewModel.saveError.value}",
            savedAndDismissed
        )

        val persisted = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(1, persisted.size)
        val savedPres = persisted.first()
        assertEquals("ডা. করিম", savedPres.prescription.doctorName)
        assertEquals(1, savedPres.medicines.size)
        val savedMed = savedPres.medicines.first()
        assertEquals("Seclo", savedMed.medicine.name)
        assertEquals(MedicineType.CAPSULE, savedMed.medicine.type)
        assertEquals(20.0, savedMed.medicine.strength!!, 1e-6)
        assertEquals("mg", savedMed.medicine.strengthUnit)
        assertEquals(14, savedMed.medicine.durationDays)
        assertEquals(1, savedMed.medicine.intervalDays)
        assertEquals(30, savedMed.medicine.stockCount)
        assertEquals("সকালে খালি পেটে", savedMed.medicine.note)
        assertEquals(1, savedMed.doseSchedules.size)
        val savedSched = savedMed.doseSchedules.first().doseSchedule
        assertEquals(DoseSlot.MORNING, savedSched.slot)
        assertEquals(0.5, savedSched.doseAmount, 1e-6)
        assertEquals(FoodRule.BEFORE, savedSched.foodRule)
    }

    @Test
    fun testValidationShowsInlineErrorsAndAutoExpandsInvalidMedicine() = runTest {
        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Save with missing Date and 0 medicines -> inline errors on both
        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("error_date_required").assertIsDisplayed()
        composeTestRule.onNodeWithTag("add_prescription_lazy_column").performScrollToIndex(2)
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("error_at_least_one_medicine")
            .assertIsDisplayed()
            .assertTextEquals("কমপক্ষে একটি ওষুধ যোগ করা আবশ্যক")
        assertTrue(prescriptionDao.getAllPrescriptionsWithMedicines().first().isEmpty())

        // 2. Set Date, add medicine, collapse it, then click Save -> auto-expands and shows Name, Type, Slots, Duration errors
        viewModel.updateDate(LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE))
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_toggle_0").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertDoesNotExist()

        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_error_name").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_error_type").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_error_slots").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_error_duration").performScrollTo().assertIsDisplayed()

        // 3. Select Morning slot without Type (so doseUnit is blank) & clear doseAmount -> shows BOTH doseAmount and doseUnit errors
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_morning").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("medicine_form_0_dose_amount_morning").performScrollTo().performTextClearance()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_form_0_error_dose_amount_morning").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_error_dose_unit_morning").performScrollTo().assertIsDisplayed()
        assertTrue(prescriptionDao.getAllPrescriptionsWithMedicines().first().isEmpty())
    }

    @Test
    fun testNewModeSaveSchedulesAlarmsDeterministicIdsShowsToastAndDismisses() = runTest {
        val futureStartDate = LocalDate.now().plusDays(2)
        val futureDateIso = futureStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModel.updateDate(futureDateIso)
        var dismissed = false

        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("doctor_name_input").performTextInput("ডা. সাদিয়া")
        composeTestRule.onNodeWithTag("add_prescription_add_medicine_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("medicine_form_0_name_input").performScrollTo().performTextInput("Napa Extra")
        composeTestRule.onNodeWithTag("medicine_form_0_type_chip_tablet").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_morning").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_night").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_duration_input").performScrollTo().performTextInput("3")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            dismissed || viewModel.saveError.value != null || viewModel.errorMessage.value != null
        }
        ShadowLooper.idleMainLooper()

        assertTrue(dismissed)
        assertEquals(
            "প্রেস্ক্রিপশন সেভ হয়েছে। 'প্রেস্ক্রিপশন' ট্যাবে দেখুন।",
            ShadowToast.getTextOfLatestToast()
        )

        val persisted = prescriptionDao.getAllPrescriptionsWithMedicines().first()
        assertEquals(1, persisted.size)
        val savedPres = persisted.first()
        assertEquals(1, savedPres.prescription.versionNumber)
        assertTrue(savedPres.prescription.isLatest)
        assertEquals(savedPres.prescription.id, savedPres.prescription.parentId)

        val schedules = savedPres.medicines.first().doseSchedules.map { it.doseSchedule }
        assertEquals(2, schedules.size)

        // 3 days x 2 slots = 6 reminders & 6 scheduled alarms with deterministic IDs
        val allReminders = reminderRepository.allReminders.first()
        assertEquals(6, allReminders.size)
        val expectedIds = buildSet {
            for (dayOffset in 0 until 3) {
                val dateStr = futureStartDate.plusDays(dayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
                for (sched in schedules) {
                    add(ReminderIdGenerator.generate(sched.id, dateStr))
                }
            }
        }
        assertEquals(expectedIds, allReminders.map { it.id }.toSet())

        val scheduledAlarmReminderIds = shadowAlarmManager.scheduledAlarms.mapNotNull { alarm ->
            Shadows.shadowOf(alarm.operation).savedIntent.getStringExtra(ReminderConstants.EXTRA_REMINDER_ID)
        }.toSet()
        assertEquals(expectedIds, scheduledAlarmReminderIds)
    }

    @Test
    fun testEditModeCreatesNewVersionCancelsOldAlarmsAndSchedulesNewAlarms() = runTest {
        val futureStartDate = LocalDate.now().plusDays(3)
        val futureDateIso = futureStartDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val v1Id = UUID.randomUUID().toString()
        val v1MedId = UUID.randomUUID().toString()
        val v1SchedId = UUID.randomUUID().toString()

        val v1Pres = PrescriptionEntity(
            id = v1Id,
            parentId = v1Id,
            versionNumber = 1,
            isLatest = true,
            date = futureDateIso,
            doctorName = "ডা. আনোয়ার",
            location = "ঢাকা",
            createdAt = "2026-11-20T08:00:00"
        )
        val v1Med = MedicineEntity(
            id = v1MedId,
            prescriptionId = v1Id,
            name = "Maxpro",
            type = MedicineType.CAPSULE,
            strength = 20.0,
            strengthUnit = "mg",
            durationDays = 2,
            intervalDays = 1,
            foodRule = FoodRule.BEFORE,
            lineageId = v1MedId,
            createdAt = "2026-11-20T08:00:00"
        )
        val v1Sched = DoseScheduleEntity(
            id = v1SchedId,
            medicineId = v1MedId,
            slot = DoseSlot.MORNING,
            time = "08:00",
            doseAmount = 1.0,
            doseUnit = "টি",
            foodRule = FoodRule.BEFORE
        )
        prescriptionRepository.saveFullPrescription(v1Pres, listOf(v1Med), listOf(v1Sched))

        val oldReminderIds = (0 until 2).map { dayOffset ->
            val dateStr = futureStartDate.plusDays(dayOffset.toLong()).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val rem = ReminderEntity.createDeterministic(
                doseScheduleId = v1SchedId,
                scheduledDate = dateStr,
                scheduledAt = "${dateStr}T08:00:00",
                status = ReminderStatus.PENDING
            )
            reminderRepository.insertReminder(rem)
            reminderScheduler.scheduleReminder(rem, v1Med.name)
            rem.id
        }
        assertEquals(2, shadowAlarmManager.scheduledAlarms.size)

        var dismissed = false
        composeTestRule.setContent {
            MediciTheme {
                AddPrescriptionListScreen(
                    viewModel = viewModel,
                    prescriptionId = v1Id,
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Card 0 is auto-expanded on edit load; add NIGHT slot & change duration to 3
        composeTestRule.onNodeWithTag("medicine_form_card_content_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("medicine_form_0_slot_chip_night").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("medicine_form_0_duration_input").performScrollTo().performTextClearance()
        composeTestRule.onNodeWithTag("medicine_form_0_duration_input").performTextInput("3")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_prescription_save_button").performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            dismissed || viewModel.saveError.value != null || viewModel.errorMessage.value != null
        }
        ShadowLooper.idleMainLooper()

        assertTrue(dismissed)
        assertEquals(
            "প্রেস্ক্রিপশন আপডেট হয়েছে। 'প্রেস্ক্রিপশন' ট্যাবে দেখুন।",
            ShadowToast.getTextOfLatestToast()
        )

        val allVersions = prescriptionDao.getPrescriptionVersions(v1Id).first()
        assertEquals(2, allVersions.size)
        val oldV1 = allVersions.first { it.id == v1Id }
        val newV2 = allVersions.first { it.id != v1Id }
        assertFalse(oldV1.isLatest)
        assertTrue(newV2.isLatest)
        assertEquals(2, newV2.versionNumber)
        assertEquals(v1Id, newV2.parentId)

        // Verify stale v1 pending reminders deleted & old alarms cancelled, while new v2 alarms (3 days x 2 slots = 6) are active
        val currentReminders = reminderRepository.allReminders.first()
        assertEquals(6, currentReminders.size)
        assertTrue(oldReminderIds.none { oldId -> currentReminders.any { it.id == oldId } })

        val activeAlarmReminderIds = shadowAlarmManager.scheduledAlarms.mapNotNull { alarm ->
            Shadows.shadowOf(alarm.operation).savedIntent.getStringExtra(ReminderConstants.EXTRA_REMINDER_ID)
        }.toSet()
        assertTrue(oldReminderIds.none { it in activeAlarmReminderIds })
        assertEquals(currentReminders.map { it.id }.toSet(), activeAlarmReminderIds)
    }
}
