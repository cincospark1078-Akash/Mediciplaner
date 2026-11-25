package com.mediciplanner.app.data.repository

import com.mediciplanner.app.data.local.dao.PrescriptionDao
import com.mediciplanner.app.data.local.entity.DoseScheduleEntity
import com.mediciplanner.app.data.local.entity.MedicineEntity
import com.mediciplanner.app.data.local.entity.PrescriptionEntity
import com.mediciplanner.app.data.local.relation.PrescriptionWithMedicines
import com.mediciplanner.app.data.util.UnitValidator
import kotlinx.coroutines.flow.Flow

class PrescriptionRepository(
    private val prescriptionDao: PrescriptionDao
) {

    val allPrescriptions: Flow<List<PrescriptionEntity>> = prescriptionDao.getAllPrescriptions()

    val allPrescriptionsWithMedicines: Flow<List<PrescriptionWithMedicines>> =
        prescriptionDao.getAllPrescriptionsWithMedicines()

    val allPrescriptionsWithMedicineCount: Flow<List<com.mediciplanner.app.data.local.relation.PrescriptionWithMedicineCount>> =
        prescriptionDao.getAllPrescriptionsWithMedicineCount()

    suspend fun getPrescriptionById(id: String): PrescriptionEntity? {
        return prescriptionDao.getPrescriptionById(id)
    }

    suspend fun getPrescriptionWithMedicinesById(id: String): PrescriptionWithMedicines? {
        return prescriptionDao.getPrescriptionWithMedicinesById(id)
    }

    suspend fun getLatestPrescriptionWithMedicinesByParentId(parentId: String): PrescriptionWithMedicines? {
        return prescriptionDao.getLatestPrescriptionWithMedicinesByParentId(parentId)
    }

    suspend fun getPrescriptionWithMedicinesByVersion(
        parentId: String,
        versionNumber: Int
    ): PrescriptionWithMedicines? {
        return prescriptionDao.getPrescriptionWithMedicinesByVersion(parentId, versionNumber)
    }

    fun getPrescriptionVersionsWithMedicines(parentId: String): Flow<List<PrescriptionWithMedicines>> {
        return prescriptionDao.getPrescriptionVersionsWithMedicines(parentId)
    }

    fun getPrescriptionVersions(parentId: String): Flow<List<PrescriptionEntity>> {
        return prescriptionDao.getPrescriptionVersions(parentId)
    }

    /**
     * Saves a new version of an existing prescription.
     * 1. Marks all existing versions with [oldPrescriptionId]'s parentId as isLatest = false.
     * 2. Inserts a new PrescriptionEntity with parentId = oldPrescription.parentId,
     *    versionNumber = oldPrescription.versionNumber + 1, and isLatest = true.
     * 3. Inserts the new/updated medicines and schedules linked to the new prescription id.
     * The old prescription and its medicines are completely preserved!
     */
    suspend fun saveNewPrescriptionVersion(
        oldPrescriptionId: String,
        newPrescription: PrescriptionEntity,
        medicines: List<MedicineEntity>,
        schedules: List<DoseScheduleEntity>
    ) {
        val oldPrescription = prescriptionDao.getPrescriptionById(oldPrescriptionId)
        val parentId = oldPrescription?.parentId ?: oldPrescriptionId
        val nextVersion = (oldPrescription?.versionNumber ?: 1) + 1

        // 1. Mark existing versions as not latest
        prescriptionDao.markAllVersionsAsOld(parentId)

        // 2. Prepare new entity with shared parentId, incremented versionNumber, and isLatest = true
        val nowIso = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        val versionedEntity = newPrescription.copy(
            id = newPrescription.id,
            parentId = parentId,
            versionNumber = nextVersion,
            isLatest = true,
            createdAt = newPrescription.createdAt.ifBlank { nowIso }
        )

        // 3. Persist new prescription and its medicines
        saveFullPrescription(
            prescription = versionedEntity,
            medicines = medicines,
            schedules = schedules
        )
    }

    suspend fun deletePrescription(prescription: PrescriptionEntity) {
        prescriptionDao.deletePrescription(prescription)
    }

    suspend fun deletePrescriptionById(id: String) {
        prescriptionDao.deleteMedicinesByPrescriptionId(id)
        prescriptionDao.deletePrescriptionById(id)
    }

    suspend fun deleteMedicinesByPrescriptionId(prescriptionId: String) {
        prescriptionDao.deleteMedicinesByPrescriptionId(prescriptionId)
    }

    suspend fun deleteMedicine(medicine: MedicineEntity) {
        prescriptionDao.deleteMedicine(medicine)
    }

    suspend fun insertMedicine(medicine: MedicineEntity) {
        prescriptionDao.insertMedicine(medicine)
    }

    suspend fun updateMedicine(medicine: MedicineEntity) {
        prescriptionDao.updateMedicine(medicine)
    }

    suspend fun getMedicineById(id: String): MedicineEntity? {
        return prescriptionDao.getMedicineById(id)
    }

    suspend fun getMedicineByScheduleId(scheduleId: String): MedicineEntity? {
        return prescriptionDao.getMedicineByScheduleId(scheduleId)
    }

    suspend fun getDoseScheduleById(id: String): DoseScheduleEntity? {
        return prescriptionDao.getDoseScheduleById(id)
    }

    suspend fun decrementStockIfAvailable(medicineId: String, doseAmount: Double = 1.0): MedicineEntity? {
        return prescriptionDao.decrementStockIfAvailable(medicineId, doseAmount)
    }

    suspend fun decrementStockWithResult(
        medicineId: String,
        doseAmount: Double = 1.0
    ): com.mediciplanner.app.data.local.dao.StockDecrementResult? {
        return prescriptionDao.decrementStockWithResult(medicineId, doseAmount)
    }

    suspend fun decrementStockForSchedule(
        doseScheduleId: String
    ): com.mediciplanner.app.data.local.dao.StockDecrementResult? {
        return prescriptionDao.decrementStockForSchedule(doseScheduleId)
    }

    /**
     * Saves a full prescription with its medicines and their corresponding dose schedules.
     * Enforces the following business validation rules:
     * 1. A Prescription MUST have at least 1 Medicine to be saved.
     * 2. A Medicine MUST have at least 1 DoseSchedule to be saved.
     * 3. Medicine strengthUnit and doseUnit MUST be valid for the MedicineType.
     *
     * @throws IllegalArgumentException if any validation rule is violated.
     */
    @Throws(IllegalArgumentException::class)
    suspend fun saveFullPrescription(
        prescription: PrescriptionEntity,
        medicines: List<MedicineEntity>,
        schedules: List<DoseScheduleEntity>
    ) {
        // Validation Rule 1: A Prescription MUST have at least 1 Medicine
        require(medicines.isNotEmpty()) {
            "A Prescription must have at least 1 Medicine to be saved."
        }

        // Group schedules by medicineId
        val schedulesByMedicine = schedules.groupBy { it.medicineId }

        for (medicine in medicines) {
            val medicineSchedules = schedulesByMedicine[medicine.id] ?: emptyList()

            // Validation Rule 2: A Medicine MUST have at least 1 DoseSchedule
            require(medicineSchedules.isNotEmpty()) {
                "Medicine '${medicine.name}' must have at least 1 DoseSchedule to be saved."
            }

            // Validation Rule 3: Unit validation for each medicine and its schedules
            UnitValidator.validateUnits(
                type = medicine.type,
                strengthUnit = medicine.strengthUnit
            )

            for (schedule in medicineSchedules) {
                UnitValidator.validateUnits(
                    type = medicine.type,
                    strengthUnit = null,
                    doseUnit = schedule.doseUnit
                )
            }
        }

        // Persist transactionally
        prescriptionDao.insertFullPrescription(
            prescription = prescription,
            medicines = medicines,
            schedules = schedules
        )
    }

    /**
     * Overload for saving full prescription using paired medicines with schedules.
     */
    @Throws(IllegalArgumentException::class)
    suspend fun saveFullPrescription(
        prescription: PrescriptionEntity,
        medicinesWithSchedules: List<Pair<MedicineEntity, List<DoseScheduleEntity>>>
    ) {
        val medicines = medicinesWithSchedules.map { it.first }
        val schedules = medicinesWithSchedules.flatMap { it.second }
        saveFullPrescription(prescription, medicines, schedules)
    }
}
