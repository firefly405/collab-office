package com.collaboffice.core.database.dao

import androidx.room.*
import com.collaboffice.core.database.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY seq ASC")
    fun observeBySession(sessionId: String): Flow<List<ChatMessageEntity>>

    @Query("SELECT MAX(seq) FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun getMaxSeq(sessionId: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<ChatMessageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Query("UPDATE chat_messages SET status = 2 WHERE sessionId = :sessionId")
    suspend fun markAllAsRead(sessionId: String)

    @Query("UPDATE chat_messages SET status = 3 WHERE msgId = :msgId")
    suspend fun recallMessage(msgId: String)
}
