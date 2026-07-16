package com.empire.myapplication.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TootDao {
    // Chat Sessions
    @Query("SELECT * FROM ChatSession WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getSessionsForOwner(ownerId: String): Flow<List<ChatSession>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSession): Long

    @Query("UPDATE ChatSession SET title = :title WHERE id = :sessionId")
    suspend fun updateSessionTitle(sessionId: Long, title: String)

    @Query("DELETE FROM ChatSession WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)

    @Query("DELETE FROM ChatSession WHERE ownerId = :ownerId")
    suspend fun clearSessionsForOwner(ownerId: String)

    @Query("DELETE FROM ChatSession")
    suspend fun clearAllSessions()

    @Query("DELETE FROM Message")
    suspend fun clearAllMessages()

    @Delete
    suspend fun deleteSession(session: ChatSession)

    // Messages
    @Query("SELECT * FROM Message WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: Long): Flow<List<Message>>

    @Query("SELECT * FROM Message WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: Long): List<Message>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message): Long

    @Query("DELETE FROM Message WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: Long)

    // Sources (مربع المصادر الديناميكي)
    @Query("SELECT * FROM SourceRef WHERE messageId = :messageId")
    fun getSourcesForMessage(messageId: Long): Flow<List<SourceRef>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSource(source: SourceRef)

    @Query("UPDATE Message SET hasSources = :hasSources WHERE id = :messageId")
    suspend fun setMessageHasSources(messageId: Long, hasSources: Boolean)

    // Memory
    @Query("SELECT * FROM UserMemory")
    fun getUserMemory(): Flow<List<UserMemory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: UserMemory)

    // Profile
    @Query("SELECT * FROM UserProfile LIMIT 1")
    fun getUserProfile(): Flow<UserProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)
}
