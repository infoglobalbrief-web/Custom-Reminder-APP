package com.remindly.app.core.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * reminders table (PRD §49) + reminder_alerts (PRD §50)
 * + occurrence completions for recurring rules.
 */

@Entity(tableName = "reminders", indices = [Index("status"), Index("startAt")])
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Personal",
    val priority: String = "NORMAL",
    /** Epoch millis of the first occurrence (local wall-clock anchored). */
    val startAt: Long,
    val endAt: Long? = null,
    val timezone: String,
    /** Serialized RepeatRule (see RepeatRule.serialize). */
    val recurrence: String = "NONE",
    val status: String = "ACTIVE",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val snoozeEnabled: Boolean = true,
    val snoozeMinutes: Int = 10,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    // Sync engine fields (PRD §53) — local-first, soft deletes
    val deletedAt: Long? = null,
    val syncVersion: Long = 1,
    val dirty: Boolean = true,
)

@Entity(
    tableName = "reminder_alerts",
    foreignKeys = [ForeignKey(
        entity = ReminderEntity::class,
        parentColumns = ["id"],
        childColumns = ["reminderId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index("reminderId")],
)
data class ReminderAlertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    /** 0 = at event time; otherwise minutes before event start (PRD §24). */
    val offsetMinutes: Int = 0,
    val enabled: Boolean = true,
)

/** Completed occurrences of a (possibly recurring) reminder. */
@Entity(
    tableName = "reminder_completions",
    primaryKeys = ["reminderId", "occurrenceAt"],
    indices = [Index("occurrenceAt")],
)
data class ReminderCompletionEntity(
    val reminderId: Long,
    /** Epoch millis of the completed occurrence. */
    val occurrenceAt: Long,
    val completedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Long,
)

/** people table (PRD §51) */
@Entity(tableName = "people", indices = [Index("name")])
data class PersonEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val relationship: String = "",
    val photoUri: String? = null,
    val phone: String? = null,
    val email: String? = null,
    val notes: String? = null,
    // birthdays table (PRD §52) folded 1:1 for MVP (person ↦ birthday)
    val birthDateEpochDay: Long? = null,
    val birthdayEnabled: Boolean = true,
    val reminderStartDays: Int = 5,
    /** Minutes since local midnight, default 22:00 (PRD §26). */
    val reminderTimeMinutes: Int = 22 * 60,
    val reminderFrequency: String = "DAILY",
    val birthdayDayAlert: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val deletedAt: Long? = null,
    val syncVersion: Long = 1,
    val dirty: Boolean = true,
)
