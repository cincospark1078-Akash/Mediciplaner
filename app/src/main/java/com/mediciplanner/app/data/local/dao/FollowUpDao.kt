package com.mediciplanner.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FollowUpDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUpEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUps(followUps: List<FollowUpEntity>)

    @Update
    suspend fun updateFollowUp(followUp: FollowUpEntity)

    @Delete
    suspend fun deleteFollowUp(followUp: FollowUpEntity)

    @Query("DELETE FROM follow_ups WHERE id = :id")
    suspend fun deleteFollowUpById(id: String)

    @Query("SELECT * FROM follow_ups WHERE id = :id")
    suspend fun getFollowUpById(id: String): FollowUpEntity?

    @Query("SELECT * FROM follow_ups ORDER BY date ASC")
    fun getAllFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE isCompleted = 0 ORDER BY date ASC")
    fun getUpcomingFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE isCompleted = 1 ORDER BY date DESC")
    fun getCompletedFollowUps(): Flow<List<FollowUpEntity>>

    @Query("SELECT * FROM follow_ups WHERE reminderEnabled = 1 AND isCompleted = 0")
    suspend fun getActiveReminderFollowUps(): List<FollowUpEntity>

    @Query("UPDATE follow_ups SET isCompleted = :isCompleted, completedAt = :completedAt WHERE id = :id")
    suspend fun setCompletionStatus(id: String, isCompleted: Boolean, completedAt: String?)
}
