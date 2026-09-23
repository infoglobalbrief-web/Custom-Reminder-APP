package com.remindly.app.core.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.remindly.app.core.data.entity.CategoryEntity
import com.remindly.app.core.data.entity.PersonEntity
import com.remindly.app.core.data.entity.ReminderAlertEntity
import com.remindly.app.core.data.entity.ReminderCompletionEntity
import com.remindly.app.core.data.entity.ReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    // ---- reminders ----------------------------------------------------

    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL ORDER BY startAt ASC")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL ORDER BY startAt ASC")
    suspend fun getAll(): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<ReminderEntity?>

    @Query("SELECT * FROM reminders WHERE deletedAt IS NULL AND status = 'ACTIVE' ORDER BY startAt ASC")
    suspend fun getActive(): List<ReminderEntity>

    @Query(
        "SELECT * FROM reminders WHERE deletedAt IS NULL AND " +
            "(title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%') " +
            "ORDER BY startAt DESC LIMIT 200"
    )
    fun search(query: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Query(
        "UPDATE reminders SET status = :status, updatedAt = :now, dirty = 1, syncVersion = syncVersion + 1 " +
            "WHERE id = :id"
    )
    suspend fun updateStatus(id: Long, status: String, now: Long = System.currentTimeMillis())

    @Query(
        "UPDATE reminders SET deletedAt = :now, updatedAt = :now, dirty = 1, syncVersion = syncVersion + 1 " +
            "WHERE id = :id"
    )
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())

    // ---- alerts (PRD §24 multiple alerts per reminder) ----------------

    @Transaction
    @Query("SELECT * FROM reminder_alerts WHERE reminderId = :reminderId")
    suspend fun alertsFor(reminderId: Long): List<ReminderAlertEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlerts(alerts: List<ReminderAlertEntity>)

    @Query("DELETE FROM reminder_alerts WHERE reminderId = :reminderId")
    suspend fun clearAlerts(reminderId: Long)

    @Transaction
    suspend fun replaceAlerts(reminderId: Long, alerts: List<ReminderAlertEntity>) {
        clearAlerts(reminderId)
        insertAlerts(alerts.map { it.copy(id = 0, reminderId = reminderId) })
    }

    // ---- completions --------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markCompleted(completion: ReminderCompletionEntity)

    @Query("DELETE FROM reminder_completions WHERE reminderId = :reminderId AND occurrenceAt = :occurrenceAt")
    suspend fun unmarkCompleted(reminderId: Long, occurrenceAt: Long)

    @Query("SELECT * FROM reminder_completions WHERE occurrenceAt BETWEEN :from AND :to")
    suspend fun completionsBetween(from: Long, to: Long): List<ReminderCompletionEntity>

    @Query("SELECT * FROM reminder_completions")
    fun observeCompletions(): Flow<List<ReminderCompletionEntity>>

    @Query("SELECT * FROM reminder_completions WHERE reminderId = :reminderId")
    suspend fun completionsFor(reminderId: Long): List<ReminderCompletionEntity>

    // ---- categories ---------------------------------------------------

    @Query("SELECT * FROM categories ORDER BY id ASC")
    fun observeCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY id ASC")
    suspend fun getCategories(): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("SELECT COUNT(*) FROM categories")
    suspend fun categoryCount(): Int
}

@Dao
interface PeopleDao {
    @Query("SELECT * FROM people WHERE deletedAt IS NULL ORDER BY name ASC")
    fun observeAll(): Flow<List<PersonEntity>>

    @Query("SELECT * FROM people WHERE deletedAt IS NULL ORDER BY name ASC")
    suspend fun getAll(): List<PersonEntity>

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): PersonEntity?

    @Query("SELECT * FROM people WHERE id = :id LIMIT 1")
    fun observeById(id: Long): Flow<PersonEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(person: PersonEntity): Long

    @Update
    suspend fun update(person: PersonEntity)

    @Query("UPDATE people SET deletedAt = :now, dirty = 1, syncVersion = syncVersion + 1 WHERE id = :id")
    suspend fun softDelete(id: Long, now: Long = System.currentTimeMillis())
}
