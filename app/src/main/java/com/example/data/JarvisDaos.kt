package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM jarvis_memory ORDER BY category ASC, key ASC")
    fun getAllMemory(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM jarvis_memory WHERE key = :key LIMIT 1")
    suspend fun getMemoryByKey(key: String): MemoryEntity?

    @Query("SELECT value FROM jarvis_memory WHERE key = :key LIMIT 1")
    suspend fun getValueByKey(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(memory: MemoryEntity)

    @Delete
    suspend fun delete(memory: MemoryEntity)

    @Query("DELETE FROM jarvis_memory WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM jarvis_memory")
    suspend fun clearAll()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM jarvis_routines ORDER BY id DESC")
    fun getAllRoutines(): Flow<List<RoutineEntity>>

    @Query("SELECT * FROM jarvis_routines WHERE isEnabled = 1")
    suspend fun getActiveRoutines(): List<RoutineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(routine: RoutineEntity): Long

    @Update
    suspend fun update(routine: RoutineEntity)

    @Delete
    suspend fun delete(routine: RoutineEntity)
}

@Dao
interface CommandHistoryDao {
    @Query("SELECT * FROM command_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentHistory(): Flow<List<CommandHistoryEntity>>

    @Insert
    suspend fun insert(entry: CommandHistoryEntity): Long

    @Query("DELETE FROM command_history")
    suspend fun clearHistory()
}

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY isTrusted DESC, name ASC")
    fun getAllContacts(): Flow<List<TrustedContactEntity>>

    @Query("SELECT * FROM trusted_contacts WHERE name LIKE '%' || :query || '%' OR phoneNumber LIKE '%' || :query || '%'")
    suspend fun searchContacts(query: String): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE isTrusted = 1")
    suspend fun getTrustedContacts(): List<TrustedContactEntity>

    @Query("SELECT * FROM trusted_contacts WHERE isTrusted = 1 AND LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getTrustedContactByName(name: String): TrustedContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: TrustedContactEntity): Long

    @Update
    suspend fun update(contact: TrustedContactEntity)

    @Delete
    suspend fun delete(contact: TrustedContactEntity)
}
