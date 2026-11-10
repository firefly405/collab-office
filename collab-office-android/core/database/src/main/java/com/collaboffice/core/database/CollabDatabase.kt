package com.collaboffice.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.collaboffice.core.database.dao.*
import com.collaboffice.core.database.entity.*

@Database(
    entities = [
        ChatSessionEntity::class,
        ChatMessageEntity::class,
        OfflineOperationEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CollabDatabase : RoomDatabase() {
    abstract fun chatSessionDao(): ChatSessionDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun offlineOpDao(): OfflineOpDao
}
