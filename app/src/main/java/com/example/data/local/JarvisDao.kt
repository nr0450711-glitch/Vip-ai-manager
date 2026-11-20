package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface JarvisDao {

    @Query("SELECT * FROM dashboard_tools ORDER BY createdAt ASC, id ASC")
    fun observeTools(): Flow<List<DashboardToolEntity>>

    @Query("SELECT COUNT(*) FROM dashboard_tools")
    suspend fun getToolCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTool(tool: DashboardToolEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTools(tools: List<DashboardToolEntity>)

    @Query("DELETE FROM dashboard_tools WHERE id = :id")
    suspend fun deleteToolById(id: Int)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC, id ASC")
    fun observeMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("UPDATE chat_messages SET content = :content, actionTag = :actionTag WHERE id = :id")
    suspend fun updateMessage(id: Long, content: String, actionTag: String?)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC, id DESC LIMIT 100")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}
