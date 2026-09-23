package com.remindly.app.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.remindly.app.core.data.dao.PeopleDao
import com.remindly.app.core.data.dao.ReminderDao
import com.remindly.app.core.data.entity.CategoryEntity
import com.remindly.app.core.data.entity.PersonEntity
import com.remindly.app.core.data.entity.ReminderAlertEntity
import com.remindly.app.core.data.entity.ReminderCompletionEntity
import com.remindly.app.core.data.entity.ReminderEntity

class Converters {
    @TypeConverter fun fromLongArray(value: LongArray?): String? = value?.joinToString(",")
    @TypeConverter fun toLongArray(value: String?): LongArray? =
        value?.split(",")?.mapNotNull { it.toLongOrNull() }?.toLongArray()
}

@Database(
    entities = [
        ReminderEntity::class,
        ReminderAlertEntity::class,
        ReminderCompletionEntity::class,
        CategoryEntity::class,
        PersonEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class RemindlyDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao
    abstract fun peopleDao(): PeopleDao

    companion object {
        // PRD §38: reminders are 100% local-first — the DB must exist before
        // anything else and survive process death.
        fun build(context: Context): RemindlyDatabase =
            Room.databaseBuilder(context.applicationContext, RemindlyDatabase::class.java, "remindly.db")
                .fallbackToDestructiveMigration()
                .build()

        /** PRD §60 / §31 default categories seeded on first launch. */
        val DEFAULT_CATEGORIES = listOf(
            CategoryEntity(name = "Personal", colorArgb = 0xFF6658E8),
            CategoryEntity(name = "Work", colorArgb = 0xFFA77AF3),
            CategoryEntity(name = "Family", colorArgb = 0xFF7BC99A),
            CategoryEntity(name = "Health", colorArgb = 0xFFF3A55A),
            CategoryEntity(name = "Other", colorArgb = 0xFF77758A),
        )
    }
}
