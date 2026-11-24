package com.mediciplanner.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "prescriptions")
data class PrescriptionEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val parentId: String = id,
    val versionNumber: Int = 1,
    val isLatest: Boolean = true,
    val date: String,
    val doctorName: String? = null,
    val location: String? = null,
    val note: String? = null,
    val createdAt: String,
    val prescriptionImagePaths: List<String> = emptyList()
)
