package com.collaboffice.core.database.dao

import androidx.room.*
import com.collaboffice.core.database.entity.OfflineOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OfflineOpDao {
    @Insert
    suspend fun enqueue(op: OfflineOperationEntity)

    @Query("SELECT * FROM offline_ops WHERE docId = :docId ORDER BY seq ASC")
    fun observeByDoc(docId: String): Flow<List<OfflineOperationEntity>>

    @Query("SELECT * FROM offline_ops WHERE synced = 0 ORDER BY seq ASC")
    suspend fun getPending(): List<OfflineOperationEntity>

    @Query("UPDATE offline_ops SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSynced(ids: List<Long>)

    @Query("DELETE FROM offline_ops WHERE synced = 1")
    suspend fun deleteSynced()
}
