package com.mediciplanner.app.data.repository

import com.mediciplanner.app.data.local.dao.FollowUpDao
import com.mediciplanner.app.data.local.entity.FollowUpEntity
import kotlinx.coroutines.flow.Flow

class FollowUpRepository(
    private val followUpDao: FollowUpDao
) {

    val allFollowUps: Flow<List<FollowUpEntity>> = followUpDao.getAllFollowUps()
    val upcomingFollowUps: Flow<List<FollowUpEntity>> = followUpDao.getUpcomingFollowUps()
    val completedFollowUps: Flow<List<FollowUpEntity>> = followUpDao.getCompletedFollowUps()

    suspend fun setCompletionStatus(id: String, isCompleted: Boolean, completedAt: String? = null) {
        followUpDao.setCompletionStatus(id, isCompleted, completedAt)
    }

    suspend fun getFollowUpById(id: String): FollowUpEntity? {
        return followUpDao.getFollowUpById(id)
    }

    suspend fun insertFollowUp(followUp: FollowUpEntity) {
        followUpDao.insertFollowUp(followUp)
    }

    suspend fun insertFollowUps(followUps: List<FollowUpEntity>) {
        followUpDao.insertFollowUps(followUps)
    }

    suspend fun updateFollowUp(followUp: FollowUpEntity) {
        followUpDao.updateFollowUp(followUp)
    }

    suspend fun deleteFollowUp(followUp: FollowUpEntity) {
        followUpDao.deleteFollowUp(followUp)
    }

    suspend fun deleteFollowUpById(id: String) {
        followUpDao.deleteFollowUpById(id)
    }
}
