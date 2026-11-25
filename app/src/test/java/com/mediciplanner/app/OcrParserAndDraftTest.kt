package com.mediciplanner.app

import android.content.Context
import android.net.Uri
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.core.app.ActivityOptionsCompat
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mediciplanner.app.core.theme.MediciTheme
import com.mediciplanner.app.core.util.OcrHelper
import com.mediciplanner.app.core.util.OcrParser
import com.mediciplanner.app.core.util.ParsedMedicine
import com.mediciplanner.app.data.local.MediciDatabase
import com.mediciplanner.app.data.local.entity.DoseSlot
import com.mediciplanner.app.data.local.entity.FoodRule
import com.mediciplanner.app.data.local.entity.MedicineType
import com.mediciplanner.app.data.repository.PrescriptionRepository
import com.mediciplanner.app.ui.screens.prescription.DraftPrescription
import com.mediciplanner.app.ui.screens.prescription.OcrReviewScreen
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselScreen
import com.mediciplanner.app.ui.screens.prescription.PrescriptionCarouselViewModel
import com.mediciplanner.app.ui.screens.prescription.components.Card1BaseInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp")
class OcrParserAndDraftTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun parseMedicinesExtractsSingleLineAndMultiLineEntries() {
        val sampleText = """
            Dr. Rahman, MBBS
            Date: 22/11/2026
            Rx
            1. Napa 500 mg - 1+0+1 - 7 days
            Cap. Seclo 20 mg
            1+0+0
            14 days
        """.trimIndent()

        val parsed = OcrParser.parseMedicines(sampleText)

        assertEquals(2, parsed.size)
        assertEquals("Napa 500 mg", parsed[0].name)
        assertEquals("1+0+1", parsed[0].dose)
        assertEquals("7 days", parsed[0].duration)
        assertNull(parsed[0].foodRule)
        assertTrue(parsed[0].rawText.contains("Napa 500 mg"))

        assertEquals("Cap. Seclo 20 mg", parsed[1].name)
        assertEquals("1+0+0", parsed[1].dose)
        assertEquals("14 days", parsed[1].duration)
        assertNull(parsed[1].foodRule)
        assertTrue(parsed[1].rawText.contains("Cap. Seclo 20 mg"))
    }

    @Test
    fun parseMedicinesFiltersKeywordsExtractsFractionalDosageFoodRulesAndIgnoresMetadata() {
        val sampleText = """
            Serial: 105
            Hospital: Square Hospital Dhaka
            Phone: 01711-223344
            Email: clinic@example.com
            Date: 20-11-2026
            Age: 42 Years
            BP: 120/80 mmHg
            Pulse: 78/min
            Diagnosis: Acute Gastritis & Fever
            Tab. Napa 500 mg 0+1/2+0 খাওয়ার পরে
            Cap. Sergel 20 mg 1+0+0 খালি পেটে
            Syp. Tofen 100ml 0+1+0 খাওয়ার আগে
            Spray Flixonase 50 mcg 1+0+1
            Inj. Ceftriaxone 1gm 1+0+0
            Drop Aristocort 0+1+0
        """.trimIndent()

        val parsed = OcrParser.parseMedicines(sampleText)

        assertEquals(6, parsed.size)

        assertEquals("Tab. Napa 500 mg", parsed[0].name)
        assertEquals("0+1/2+0", parsed[0].dose)
        assertEquals(FoodRule.AFTER, parsed[0].foodRule)

        assertEquals("Cap. Sergel 20 mg", parsed[1].name)
        assertEquals("1+0+0", parsed[1].dose)
        assertEquals(FoodRule.EMPTY, parsed[1].foodRule)

        assertEquals("Syp. Tofen 100ml", parsed[2].name)
        assertEquals("0+1+0", parsed[2].dose)
        assertEquals(FoodRule.BEFORE, parsed[2].foodRule)

        assertEquals("Spray Flixonase 50 mcg", parsed[3].name)
        assertEquals("1+0+1", parsed[3].dose)

        assertEquals("Inj. Ceftriaxone 1gm", parsed[4].name)
        assertEquals("1+0+0", parsed[4].dose)

        assertEquals("Drop Aristocort", parsed[5].name)
        assertEquals("0+1+0", parsed[5].dose)
    }

    @Test
    fun toDraftMedicineMapsNameTypeStrengthFractionDoseFoodRuleAndDuration() {
        val parsed = ParsedMedicine(
            name = "Cap. Seclo 20 mg",
            dose = "0+1/2+0",
            foodRule = FoodRule.EMPTY,
            rawText = "Cap. Seclo 20 mg 0+1/2+0 খালি পেটে 14 days",
            duration = "14 days"
        )

        val draft = OcrParser.toDraftMedicine(parsed)

        assertEquals("Seclo", draft.name)
        assertEquals(MedicineType.CAPSULE, draft.type)
        assertEquals("20", draft.strength)
        assertEquals("mg", draft.strengthUnit)
        assertEquals("14", draft.durationDays)
        assertEquals(FoodRule.EMPTY, draft.foodRule)
        assertEquals(1, draft.schedules.size)
        assertEquals(DoseSlot.NOON, draft.schedules[0].slot)
        assertEquals("0.5", draft.schedules[0].doseAmount)
        assertEquals(FoodRule.EMPTY, draft.schedules[0].foodRule)
    }

    @Test
    fun parseMedicinesFiltersNoiseLinesCleansBracketsAndSplitsCombinationStrength() {
        val sampleText = """
            S+ fD
            S + fD
            + SfD
            0+0
            0 + 0
            Random non medicine text 1+0+1
            (Tab. Preclot AS   75 mg + 75 mg) 0+1+0 খাওয়ার পরে
            [Tab. Napa   500 mg] 1+0+1
            Amoxicillin 1+0+1 খাওয়ার আগে
        """.trimIndent()

        val parsed = OcrParser.parseMedicines(sampleText)

        assertEquals(3, parsed.size)

        assertEquals("Tab. Preclot AS", parsed[0].name)
        assertEquals("75 mg + 75 mg", parsed[0].strength)
        assertEquals("0+1+0", parsed[0].dose)
        assertEquals(FoodRule.AFTER, parsed[0].foodRule)

        assertEquals("Tab. Napa 500 mg", parsed[1].name)
        assertEquals("1+0+1", parsed[1].dose)

        assertEquals("Amoxicillin", parsed[2].name)
        assertEquals("1+0+1", parsed[2].dose)
        assertEquals(FoodRule.BEFORE, parsed[2].foodRule)
    }

    @Test
    fun ocrReviewScreenOnlyDisplaysValidMedicinesAndShowsSplitStrengthBadge() {
        val mixedItems = listOf(
            ParsedMedicine("S+ fD", null, FoodRule.AFTER, "S+ fD"),
            ParsedMedicine("+ SfD", null, FoodRule.AFTER, "+ SfD"),
            ParsedMedicine("0+0", null, FoodRule.AFTER, "0+0"),
            ParsedMedicine(
                name = "Tab. Preclot AS",
                dose = "0+1+0",
                foodRule = FoodRule.AFTER,
                rawText = "Tab. Preclot AS 75 mg + 75 mg 0+1+0",
                strength = "75 mg + 75 mg"
            )
        )

        composeTestRule.setContent {
            MediciTheme {
                OcrReviewScreen(
                    imageUri = null,
                    medicines = mixedItems,
                    onUpdateMedicine = { _, _, _, _ -> },
                    onDeleteMedicine = {},
                    onAddNewMedicine = {},
                    onAddAll = {},
                    onCancel = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ocr_review_item_0").assertIsDisplayed()
        composeTestRule.onNodeWithTag("ocr_review_item_1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("ocr_review_name_input_0")
            .assertTextContains("Tab. Preclot AS")
        composeTestRule.onNodeWithTag("ocr_review_strength_badge_0")
            .assertIsDisplayed()
            .assertTextEquals("75 mg + 75 mg")
    }

    @Test
    fun card1ShowsParsedMedicinesAndTriggersAddToPrescription() {
        val parsedList = listOf(
            ParsedMedicine("Napa 500 mg", "1+0+1", FoodRule.AFTER, "Napa 500 mg - 1+0+1 - 7 days", "7 days")
        )
        var addedMedicine: ParsedMedicine? = null

        composeTestRule.setContent {
            MediciTheme {
                Card1BaseInfo(
                    draftPrescription = DraftPrescription(date = "2026-11-22"),
                    onDateChanged = {},
                    onDoctorNameChanged = {},
                    onLocationChanged = {},
                    extractedText = "Napa 500 mg - 1+0+1 - 7 days",
                    parsedMedicines = parsedList,
                    onAddParsedMedicine = { addedMedicine = it }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ocr_parsed_medicine_text_0")
            .performScrollTo()
            .assertIsDisplayed()
            .assertTextContains("Napa 500 mg - 1+0+1 - 7 days", substring = true)

        composeTestRule.onNodeWithTag("ocr_add_to_prescription_button_0")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertNotNull(addedMedicine)
        assertEquals("Napa 500 mg", addedMedicine?.name)
    }

    @Test
    fun unifiedAddPrescriptionBoxOpensBottomSheetWithScanAndUploadOptions() {
        composeTestRule.setContent {
            MediciTheme {
                Card1BaseInfo(
                    draftPrescription = DraftPrescription(date = "2026-11-22"),
                    onDateChanged = {},
                    onDoctorNameChanged = {},
                    onLocationChanged = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        // Old separate elements must be removed
        composeTestRule.onNodeWithTag("scan_prescription_button").assertDoesNotExist()
        composeTestRule.onNodeWithTag("add_prescription_image_button").assertDoesNotExist()

        // Unified box must be displayed and open ModalBottomSheet on tap
        composeTestRule.onNodeWithTag("add_prescription_unified_box")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sheet_scan_prescription_option").assertIsDisplayed()
        composeTestRule.onNodeWithTag("sheet_upload_gallery_option").assertIsDisplayed()
    }

    @Test
    fun card1DisplaysScanningIndicatorTextWhenOcrIsRunning() {
        composeTestRule.setContent {
            MediciTheme {
                Card1BaseInfo(
                    draftPrescription = DraftPrescription(date = "2026-11-22"),
                    onDateChanged = {},
                    onDoctorNameChanged = {},
                    onLocationChanged = {},
                    isScanningOcr = true
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ocr_scanning_indicator")
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("ocr_scanning_text")
            .assertIsDisplayed()
            .assertTextEquals("ছবি স্ক্যান হচ্ছে...")
    }

    @Test
    fun card1GalleryUploadCopiesImagesAndTriggersOcr() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val source1 = File(context.cacheDir, "gallery_test_1.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val source2 = File(context.cacheDir, "gallery_test_2.jpg").apply { writeBytes(byteArrayOf(4, 5, 6)) }
        val uri1 = Uri.fromFile(source1)
        val uri2 = Uri.fromFile(source2)

        val addedFileNames = mutableListOf<String>()
        val ocrTriggeredUris = mutableListOf<Uri>()

        val fakeRegistryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    requestCode: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?
                ) {
                    @Suppress("UNCHECKED_CAST")
                    dispatchResult(requestCode, listOf(uri1, uri2) as O)
                }
            }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalActivityResultRegistryOwner provides fakeRegistryOwner) {
                MediciTheme {
                    Card1BaseInfo(
                        draftPrescription = DraftPrescription(date = "2026-11-22"),
                        onDateChanged = {},
                        onDoctorNameChanged = {},
                        onLocationChanged = {},
                        onImagesAdded = { addedFileNames.addAll(it) },
                        onScanPrescriptionImages = { ocrTriggeredUris.addAll(it) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("add_prescription_unified_box")
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("sheet_upload_gallery_option")
            .performClick()
        composeTestRule.waitForIdle()

        assertEquals(2, addedFileNames.size)
        assertEquals(2, ocrTriggeredUris.size)
        addedFileNames.forEach { fileName ->
            val copied = File(File(context.filesDir, "prescription_images"), fileName)
            assertTrue("Copied image should exist in internal storage", copied.exists())
            copied.delete()
        }
        source1.delete()
        source2.delete()
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun processOcrImagesRunsSequentiallyConcatenatesTextAndOpensReviewScreen() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val startedUris = mutableListOf<Uri>()
            val pendingCallbacks = mutableListOf<(String) -> Unit>()

            val fakeOcrHelper = object : OcrHelper(context) {
                override fun recognizeText(
                    imageUri: Uri,
                    onSuccess: (String) -> Unit,
                    onFailure: (Exception) -> Unit
                ) {
                    startedUris.add(imageUri)
                    pendingCallbacks.add(onSuccess)
                }
            }

            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                ocrHelper = fakeOcrHelper
            )

            val uri1 = Uri.parse("file:///tmp/rx_page_1.jpg")
            val uri2 = Uri.parse("file:///tmp/rx_page_2.jpg")

            viewModel.processOcrImages(listOf(uri1, uri2))

            // Only first image should start immediately; second waits for first to complete
            assertTrue(viewModel.isScanningOcr.value)
            assertEquals(listOf(uri1), startedUris)

            pendingCallbacks[0].invoke("1. Napa 500 mg - 1+0+1 - 7 days")

            // After first completes, scanning remains active and second image starts
            assertTrue(viewModel.isScanningOcr.value)
            assertEquals(listOf(uri1, uri2), startedUris)

            pendingCallbacks[1].invoke("Cap. Seclo 20 mg\n1+0+0\n14 days")

            // After all images complete, loading clears, parsed medicines are merged, and review opens
            assertFalse(viewModel.isScanningOcr.value)
            assertTrue(viewModel.extractedText.value.contains("Napa 500 mg"))
            assertTrue(viewModel.extractedText.value.contains("Cap. Seclo 20 mg"))
            assertEquals(2, viewModel.parsedMedicines.value.size)
            assertEquals("Napa 500 mg", viewModel.parsedMedicines.value[0].name)
            assertEquals("Cap. Seclo 20 mg", viewModel.parsedMedicines.value[1].name)
            assertTrue(viewModel.showOcrReview.value)
            assertEquals(2, viewModel.reviewMedicines.value.size)
            assertEquals(uri1, viewModel.pendingOcrImageUri.value)
        } finally {
            db.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun ocrReviewScreenDisplaysThumbnailAllowsEditAddDeleteAndAddAllNavigatesToCard2() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val fakeOcrHelper = object : OcrHelper(context) {
                override fun recognizeText(
                    imageUri: Uri,
                    onSuccess: (String) -> Unit,
                    onFailure: (Exception) -> Unit
                ) {
                    onSuccess("Tab. Napa 500 mg 1+0+1 খাওয়ার পরে\nCap. Seclo 20 mg 1+0+0 খাওয়ার আগে")
                }
            }

            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                ocrHelper = fakeOcrHelper
            )
            viewModel.processOcrImage(Uri.parse("file:///tmp/rx_review_test.jpg"))

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {}
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Verify Review Screen & Original Image Thumbnail are visible
            composeTestRule.onNodeWithTag("ocr_review_screen").assertIsDisplayed()
            composeTestRule.onNodeWithTag("ocr_review_image_thumbnail").assertIsDisplayed()
            composeTestRule.onNodeWithTag("ocr_review_item_0").assertIsDisplayed()
            composeTestRule.onNodeWithTag("ocr_review_item_1").assertIsDisplayed()

            // Edit first medicine name and dose
            composeTestRule.onNodeWithTag("ocr_review_name_input_0")
                .performTextReplacement("Tab. Napa Extra 500 mg")
            composeTestRule.onNodeWithTag("ocr_review_dose_input_0")
                .performTextReplacement("0+1/2+0")
            composeTestRule.waitForIdle()

            // Change FoodRule via dropdown
            composeTestRule.onNodeWithTag("ocr_review_food_rule_dropdown_0")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("ocr_review_food_rule_option_0_empty")
                .performClick()
            composeTestRule.waitForIdle()

            // Delete second item
            composeTestRule.onNodeWithTag("ocr_review_delete_1")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            assertEquals(1, viewModel.reviewMedicines.value.size)

            // Add a manual medicine via "Add New Medicine"
            composeTestRule.onNodeWithTag("ocr_review_add_new_button")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            assertEquals(2, viewModel.reviewMedicines.value.size)

            composeTestRule.onNodeWithTag("ocr_review_name_input_1")
                .performScrollTo()
                .performTextReplacement("Syp. Adovas")
            composeTestRule.onNodeWithTag("ocr_review_dose_input_1")
                .performScrollTo()
                .performTextReplacement("1+0+1")
            composeTestRule.waitForIdle()

            // Manual item has no FoodRule yet -> clicking Add All must block with validation error
            composeTestRule.onNodeWithTag("ocr_review_add_all_button")
                .performClick()
            composeTestRule.waitForIdle()
            assertTrue(viewModel.showOcrReview.value)
            composeTestRule.onNodeWithTag("ocr_review_validation_error")
                .assertIsDisplayed()
                .assertTextContains("অনুগ্রহ করে সব ওষুধের খাবারের নিয়ম নির্বাচন করুন।", substring = true)

            // Select FoodRule for manually added item
            composeTestRule.onNodeWithTag("ocr_review_food_rule_dropdown_1")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("ocr_review_food_rule_option_1_after")
                .performClick()
            composeTestRule.waitForIdle()

            // Click "সব যোগ করুন" -> adds all edited medicines to Draft and navigates to Card 2
            composeTestRule.onNodeWithTag("ocr_review_add_all_button")
                .performClick()
            composeTestRule.waitForIdle()

            assertFalse(viewModel.showOcrReview.value)
            assertEquals(2, viewModel.draftPrescription.value.medicines.size)
            assertEquals("Napa Extra", viewModel.draftPrescription.value.medicines[0].name)
            assertEquals(FoodRule.EMPTY, viewModel.draftPrescription.value.medicines[0].foodRule)
            assertEquals("0.5", viewModel.draftPrescription.value.medicines[0].schedules[0].doseAmount)
            assertEquals("Adovas", viewModel.draftPrescription.value.medicines[1].name)

            composeTestRule.onNodeWithTag("carousel_card_2").assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @Test
    fun ocrReviewScreenCancelDiscardsAndReturnsToCard1() {
        var cancelled = false
        composeTestRule.setContent {
            MediciTheme {
                OcrReviewScreen(
                    imageUri = Uri.parse("file:///tmp/rx_cancel.jpg"),
                    medicines = listOf(
                        ParsedMedicine("Tab. Napa 500 mg", "1+0+1", FoodRule.AFTER, "Tab. Napa 500 mg 1+0+1")
                    ),
                    onUpdateMedicine = { _, _, _, _ -> },
                    onDeleteMedicine = {},
                    onAddNewMedicine = {},
                    onAddAll = {},
                    onCancel = { cancelled = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("ocr_review_cancel_button")
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertTrue(cancelled)
    }

    @Test
    fun card2AccordionDisplaysAddedMedicinesLoadsOnTapAndDeletesItem() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao())
            )
            viewModel.updateDate("2026-11-23")
            viewModel.addAllParsedMedicinesToDraft(
                listOf(
                    ParsedMedicine("Tab. Napa 500 mg", "1+0+1", FoodRule.AFTER, "Tab. Napa 500 mg 1+0+1"),
                    ParsedMedicine("Cap. Seclo 20 mg", "1+0+0", FoodRule.BEFORE, "Cap. Seclo 20 mg 1+0+0")
                )
            )

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {},
                        initialPage = 1
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Horizontal chip row must not exist
            composeTestRule.onNodeWithTag("added_medicines_chip_row").assertDoesNotExist()

            // Accordion button must be displayed with count (২)
            composeTestRule.onNodeWithTag("added_medicines_accordion_button")
                .assertIsDisplayed()
                .assertTextContains("যুক্ত করা ওষুধসমূহ (২)", substring = true)
                .performClick()
            composeTestRule.waitForIdle()

            // Vertical list shows all added medicines
            composeTestRule.onNodeWithTag("added_medicines_accordion_list").assertIsDisplayed()
            composeTestRule.onNodeWithTag("added_medicine_item_0").assertIsDisplayed()
            composeTestRule.onNodeWithTag("added_medicine_item_1").assertIsDisplayed()

            // Tapping second medicine loads it into Card 2 form
            composeTestRule.onNodeWithTag("added_medicine_item_1").performClick()
            composeTestRule.waitForIdle()

            assertEquals(1, viewModel.selectedMedicineIndex.value)
            assertEquals("Seclo", viewModel.draftMedicine.value.name)
            composeTestRule.onNodeWithTag("medicine_name_input")
                .performScrollTo()
                .assertTextContains("Seclo", substring = true)

            // Deleting first medicine removes it and updates count to (১)
            composeTestRule.onNodeWithTag("added_medicine_delete_0")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()

            assertEquals(1, viewModel.draftPrescription.value.medicines.size)
            assertEquals("Seclo", viewModel.draftPrescription.value.medicines[0].name)
            assertEquals(0, viewModel.selectedMedicineIndex.value)
            composeTestRule.onNodeWithTag("added_medicines_accordion_button")
                .performScrollTo()
                .assertTextContains("যুক্ত করা ওষুধসমূহ (১)", substring = true)
        } finally {
            db.close()
        }
    }

    @Test
    fun carouselWithEmptyDateAndNonZeroInitialPageStartsOnCard1() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao())
            )
            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {},
                        initialPage = 3
                    )
                }
            }
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_1").assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @Test
    fun topBarCard1ShortcutJumpsFromCard4DirectlyToCard1() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val fakeOcrHelper = object : OcrHelper(context) {
                override fun recognizeText(
                    imageUri: Uri,
                    onSuccess: (String) -> Unit,
                    onFailure: (Exception) -> Unit
                ) {
                    onSuccess("Tab. Napa 500 mg 1+0+1 খাওয়ার পরে")
                }
            }
            val scanVm = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                ocrHelper = fakeOcrHelper
            )
            scanVm.processOcrImage(Uri.parse("file:///tmp/rx_card4_stuck.jpg"))

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = scanVm,
                        onDismiss = {}
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Tap Add All from OCR Review -> jumps to Card 2 with empty Date
            composeTestRule.onNodeWithTag("ocr_review_add_all_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_2").assertIsDisplayed()
            composeTestRule.onNodeWithTag("scan_date_prompt_banner").assertIsDisplayed()

            // Step through Card 2 -> Card 3 -> Card 4 without setting Date
            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_3").assertIsDisplayed()

            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_4").assertIsDisplayed()

            // Verify Top Bar shortcut works from Card 4 -> jumps directly to Card 1
            composeTestRule.onNodeWithTag("carousel_step_badge").assertTextEquals("ধাপ ৪/৪")
            composeTestRule.onNodeWithTag("carousel_card1_shortcut").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_1").assertIsDisplayed()
        } finally {
            db.close()
        }
    }

    @Test
    fun card4SaveWithoutDateShowsErrorAndGoToCard1ButtonThenAllowsSaving() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val fakeOcrHelper = object : OcrHelper(context) {
                override fun recognizeText(
                    imageUri: Uri,
                    onSuccess: (String) -> Unit,
                    onFailure: (Exception) -> Unit
                ) {
                    onSuccess("Tab. Napa 500 mg 1+0+1 খাওয়ার পরে")
                }
            }
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                ocrHelper = fakeOcrHelper
            )
            viewModel.processOcrImage(Uri.parse("file:///tmp/rx_save_no_date.jpg"))

            var dismissed = false
            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = { dismissed = true }
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Scan Review -> Add All -> Card 2
            composeTestRule.onNodeWithTag("ocr_review_add_all_button").performClick()
            composeTestRule.waitForIdle()

            // Card 2 -> Card 3 -> Card 4
            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_4").assertIsDisplayed()

            // Enter duration on Card 4
            composeTestRule.onNodeWithTag("duration_days_input").performTextReplacement("7")
            composeTestRule.waitForIdle()

            // Tap Save ("যাচাই ও সংরক্ষণ") with Date still missing
            composeTestRule.onNodeWithTag("footer_review_save_button").performClick()
            composeTestRule.waitForIdle()

            // Error message and "তারিখ সেট করতে কার্ড ১-এ যান" button must be displayed
            composeTestRule.onNodeWithTag("carousel_error_banner")
                .assertIsDisplayed()
                .assertTextContains("অনুগ্রহ করে তারিখ সিলেক্ট করুন", substring = true)
            composeTestRule.onNodeWithTag("go_to_card1_from_error_button")
                .assertIsDisplayed()
                .assertTextContains("তারিখ সেট করতে কার্ড ১-এ যান", substring = true)
                .performClick()
            composeTestRule.waitForIdle()

            // Clicking button jumps directly to Card 1 and highlights date error
            composeTestRule.onNodeWithTag("carousel_card_1").assertIsDisplayed()
            composeTestRule.onNodeWithTag("error_date_required").assertIsDisplayed()
            assertFalse(dismissed)

            // User sets Date on Card 1, then saves
            viewModel.updateDate("2026-11-23")
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("error_date_required").assertDoesNotExist()
        } finally {
            db.close()
        }
    }

    @Test
    fun medicineSwitcherAppearsOnCards234PreservesSelectionAndLoadsMedicineData() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao())
            )
            viewModel.updateDate("2026-11-23")
            viewModel.addAllParsedMedicinesToDraft(
                listOf(
                    ParsedMedicine(
                        name = "Tab. Preclot AS 75 mg",
                        dose = "0+1+0",
                        foodRule = FoodRule.AFTER,
                        rawText = "Tab. Preclot AS 75 mg 0+1+0",
                        duration = "10 days"
                    ),
                    ParsedMedicine(
                        name = "Cap. RTV Eze 20 mg",
                        dose = "1+0+1",
                        foodRule = FoodRule.BEFORE,
                        rawText = "Cap. RTV Eze 20 mg 1+0+1",
                        duration = "14 days"
                    )
                )
            )

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {},
                        initialPage = 1
                    )
                }
            }
            composeTestRule.waitForIdle()

            // Card 2: Switcher visible with label "ওষুধ নির্বাচন করুন" and both medicines
            composeTestRule.onNodeWithTag("carousel_card_2").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher_label").assertTextEquals("ওষুধ নির্বাচন করুন")
            composeTestRule.onNodeWithTag("medicine_switcher_chip_0")
                .assertTextContains("Preclot AS", substring = true)
                .assertIsSelected()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1")
                .assertTextContains("RTV Eze", substring = true)
                .assertIsNotSelected()

            // Switch to "RTV Eze" on Card 2 -> loads Name, Strength, and preserves selection
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1").assertIsSelected()
            composeTestRule.onNodeWithTag("medicine_name_input").assertTextContains("RTV Eze", substring = true)
            composeTestRule.onNodeWithTag("strength_input").assertTextContains("20", substring = true)

            // Navigate to Card 3 -> Switcher still visible, "RTV Eze" remains selected, schedule loaded
            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_3").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1").assertIsSelected()
            composeTestRule.onNodeWithTag("schedule_item_morning")
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("schedule_item_night")
                .performScrollTo()
                .assertIsDisplayed()

            // Switch back to "Preclot AS" on Card 3 -> loads Noon-only schedule
            composeTestRule.onNodeWithTag("medicine_switcher_chip_0")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_0").assertIsSelected()
            composeTestRule.onNodeWithTag("schedule_item_noon")
                .performScrollTo()
                .assertIsDisplayed()
            composeTestRule.onNodeWithTag("schedule_item_morning").assertDoesNotExist()

            // Navigate to Card 4 -> Switcher still visible, "Preclot AS" selected, Duration = 10
            composeTestRule.onNodeWithTag("footer_next_button").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("carousel_card_4").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher").assertIsDisplayed()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_0").assertIsSelected()
            composeTestRule.onNodeWithTag("duration_days_input")
                .performScrollTo()
                .assertTextContains("10", substring = true)

            // Switch to "RTV Eze" on Card 4 -> loads Duration = 14
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("medicine_switcher_chip_1").assertIsSelected()
            composeTestRule.onNodeWithTag("duration_days_input")
                .performScrollTo()
                .assertTextContains("14", substring = true)
        } finally {
            db.close()
        }
    }

    @Test
    fun card4NextMedicineButtonSavesCurrentAndLoadsNextThenHidesOnLastMedicine() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = PrescriptionRepository(db.prescriptionDao())
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = repository,
                ocrHelper = null
            )
            viewModel.updateDate("2026-11-23")
            viewModel.addAllParsedMedicinesToDraft(
                listOf(
                    ParsedMedicine(
                        name = "Tab. Preclot AS 75 mg",
                        dose = "0+1+0",
                        foodRule = FoodRule.AFTER,
                        rawText = "Tab. Preclot AS 75 mg 0+1+0",
                        duration = "10 days"
                    ),
                    ParsedMedicine(
                        name = "Cap. RTV Eze 20 mg",
                        dose = "1+0+1",
                        foodRule = FoodRule.BEFORE,
                        rawText = "Cap. RTV Eze 20 mg 1+0+1",
                        duration = "14 days"
                    )
                )
            )

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {},
                        initialPage = 3
                    )
                }
            }
            composeTestRule.waitForIdle()

            // First medicine on Card 4: Next Medicine button is visible below Add More / Review & Save
            composeTestRule.onNodeWithTag("carousel_card_4").assertIsDisplayed()
            composeTestRule.onNodeWithTag("footer_add_more_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("footer_review_save_button").assertIsDisplayed()
            composeTestRule.onNodeWithTag("footer_next_medicine_button")
                .assertIsDisplayed()
                .assertTextContains("পরবর্তী ওষুধ", substring = true)

            // Update duration and notes for first medicine
            composeTestRule.onNodeWithTag("duration_days_input")
                .performScrollTo()
                .performTextReplacement("12")
            composeTestRule.onNodeWithTag("notes_input")
                .performScrollTo()
                .performTextReplacement("সকালে খাবেন")
            composeTestRule.waitForIdle()

            // Click Next Medicine -> saves first medicine's duration & notes and loads second medicine
            composeTestRule.onNodeWithTag("footer_next_medicine_button").performClick()
            composeTestRule.waitForIdle()

            assertEquals(1, viewModel.selectedMedicineIndex.value)
            assertEquals("12", viewModel.draftPrescription.value.medicines[0].durationDays)
            assertEquals("সকালে খাবেন", viewModel.draftPrescription.value.medicines[0].note)

            composeTestRule.onNodeWithTag("medicine_switcher_chip_1").assertIsSelected()
            composeTestRule.onNodeWithTag("duration_days_input")
                .performScrollTo()
                .assertTextContains("14", substring = true)
            // Last medicine -> Next Medicine button is hidden
            composeTestRule.onNodeWithTag("footer_next_medicine_button").assertDoesNotExist()
        } finally {
            db.close()
        }
    }

    @Test
    fun card4NextMedicineButtonHiddenWhenOnlyOneMedicine() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repository = PrescriptionRepository(db.prescriptionDao())
            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = repository,
                ocrHelper = null
            )
            viewModel.updateDate("2026-11-23")
            viewModel.addAllParsedMedicinesToDraft(
                listOf(
                    ParsedMedicine(
                        name = "Tab. Preclot AS 75 mg",
                        dose = "0+1+0",
                        foodRule = FoodRule.AFTER,
                        rawText = "Tab. Preclot AS 75 mg 0+1+0",
                        duration = "10 days"
                    )
                )
            )

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {},
                        initialPage = 3
                    )
                }
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("carousel_card_4").assertIsDisplayed()
            composeTestRule.onNodeWithTag("footer_next_medicine_button").assertDoesNotExist()
        } finally {
            db.close()
        }
    }

    @Test
    fun ocrParserLeavesFoodRuleNullWhenMissingAndMapsExplicitRulesOnly() {
        val sampleText = """
            Tab. Napa 500 mg 1+0+1
            Cap. Seclo 20 mg 1+0+0 খাওয়ার আগে
            Tab. Ace Plus 500 mg 1+0+1 খাওয়ার পরে
            Tab. Metformin 500 mg 1+0+1 খাবারের সাথে
            Cap. Sergel 20 mg 1+0+0 খালি পেটে
        """.trimIndent()

        val parsed = OcrParser.parseMedicines(sampleText)
        assertEquals(5, parsed.size)
        assertNull("Missing food rule must not default to AFTER", parsed[0].foodRule)
        assertEquals(FoodRule.BEFORE, parsed[1].foodRule)
        assertEquals(FoodRule.AFTER, parsed[2].foodRule)
        assertEquals(FoodRule.WITH, parsed[3].foodRule)
        assertEquals(FoodRule.EMPTY, parsed[4].foodRule)
    }

    @Test
    fun ocrReviewScreenShowsWarningChipPreSelectsNothingAndBlocksSaveUntilRuleSelected() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, MediciDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val fakeOcrHelper = object : OcrHelper(context) {
                override fun recognizeText(
                    imageUri: Uri,
                    onSuccess: (String) -> Unit,
                    onFailure: (Exception) -> Unit
                ) {
                    onSuccess("Tab. Napa 500 mg 1+0+1\nCap. Seclo 20 mg 1+0+0 খালি পেটে")
                }
            }

            val viewModel = PrescriptionCarouselViewModel(
                prescriptionRepository = PrescriptionRepository(db.prescriptionDao()),
                ocrHelper = fakeOcrHelper
            )
            viewModel.processOcrImage(Uri.parse("file:///tmp/rx_missing_food_rule.jpg"))

            composeTestRule.setContent {
                MediciTheme {
                    PrescriptionCarouselScreen(
                        viewModel = viewModel,
                        onDismiss = {}
                    )
                }
            }
            composeTestRule.waitForIdle()

            // First medicine has no food rule -> warning chip visible, nothing pre-selected, second has no warning chip
            composeTestRule.onNodeWithTag("ocr_review_food_rule_warning_chip_0")
                .assertIsDisplayed()
                .assertTextContains("নিয়ম নির্বাচন করুন", substring = true)
            composeTestRule.onNodeWithTag("ocr_review_food_rule_warning_chip_1")
                .assertDoesNotExist()
            composeTestRule.onNodeWithTag("ocr_review_food_rule_dropdown_0")
                .assert(
                    androidx.compose.ui.test.SemanticsMatcher.expectValue(
                        androidx.compose.ui.semantics.SemanticsProperties.EditableText,
                        androidx.compose.ui.text.AnnotatedString("")
                    )
                )
            assertNull(viewModel.reviewMedicines.value[0].foodRule)
            assertEquals(FoodRule.EMPTY, viewModel.reviewMedicines.value[1].foodRule)

            // Clicking Add All must block and display error banner
            composeTestRule.onNodeWithTag("ocr_review_add_all_button").performClick()
            composeTestRule.waitForIdle()

            assertTrue(viewModel.showOcrReview.value)
            composeTestRule.onNodeWithTag("ocr_review_validation_error")
                .assertIsDisplayed()
                .assertTextContains("অনুগ্রহ করে সব ওষুধের খাবারের নিয়ম নির্বাচন করুন।", substring = true)

            // Explicitly choose BEFORE for first medicine -> warning chip & error clear, Add All succeeds
            composeTestRule.onNodeWithTag("ocr_review_food_rule_dropdown_0")
                .performScrollTo()
                .performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("ocr_review_food_rule_option_0_before")
                .performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("ocr_review_food_rule_warning_chip_0").assertDoesNotExist()

            composeTestRule.onNodeWithTag("ocr_review_add_all_button").performClick()
            composeTestRule.waitForIdle()

            assertFalse(viewModel.showOcrReview.value)
            assertEquals(2, viewModel.draftPrescription.value.medicines.size)
            assertEquals(FoodRule.BEFORE, viewModel.draftPrescription.value.medicines[0].foodRule)
            assertEquals(FoodRule.EMPTY, viewModel.draftPrescription.value.medicines[1].foodRule)
        } finally {
            db.close()
        }
    }
}
