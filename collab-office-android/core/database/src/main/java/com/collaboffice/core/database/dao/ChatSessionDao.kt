package com.collaboffice.core.database.dao

import androidx.room.*
import com.collaboffice.core.database.entity.ChatSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatSessionDao {
    @Query("SELECT * FROM chat_sessions ORDER BY lastMessageTime DESC")
    fun observeAll(): Flow<List<ChatSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sessions: List<ChatSessionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ChatSessionEntity)

    @Query("UPDATE chat_sessions SET unreadCount = 0 WHERE sessionId = :sessionId")
    suspend fun clearUnread(sessionId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAll()
}
