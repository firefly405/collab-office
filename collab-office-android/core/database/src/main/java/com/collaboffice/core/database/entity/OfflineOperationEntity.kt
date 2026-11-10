package com.collaboffice.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "offline_ops")
data class OfflineOperationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val docId: String = "",
    val seq: Long = 0,
    val operationJson: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val synced: Boolean = false
)
