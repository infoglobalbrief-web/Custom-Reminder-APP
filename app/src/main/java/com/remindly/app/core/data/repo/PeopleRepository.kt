package com.remindly.app.core.data.repo

import com.remindly.app.core.data.RemindlyDatabase
import com.remindly.app.core.data.entity.PersonEntity
import com.remindly.app.core.domain.model.Person
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime

/** people + birthdays repository (PRD §25–26, §51–52). */
class PeopleRepository(private val db: RemindlyDatabase) {

    private val dao get() = db.peopleDao()

    fun observePeople(): Flow<List<Person>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    suspend fun get(id: Long): Person? = dao.getById(id)?.toDomain()

    suspend fun getAll(): List<Person> = dao.getAll().map { it.toDomain() }

    suspend fun save(person: Person): Long {
        val entity = person.toEntity()
        return if (entity.id == 0L) {
            dao.insert(entity)
        } else {
            dao.update(entity.copy(updatedAt = System.currentTimeMillis(), dirty = true))
            entity.id
        }
    }

    suspend fun delete(id: Long) = dao.softDelete(id)

    private fun PersonEntity.toDomain() = Person(
        id = id,
        name = name,
        relationship = relationship,
        photoUri = photoUri,
        phone = phone,
        email = email,
        notes = notes,
        birthDate = birthDateEpochDay?.let(LocalDate::ofEpochDay),
        birthdayEnabled = birthdayEnabled,
        reminderStartDays = reminderStartDays,
        reminderTime = LocalTime.of(reminderTimeMinutes / 60, reminderTimeMinutes % 60),
        birthdayDayAlert = birthdayDayAlert,
        createdAt = createdAt,
    )

    private fun Person.toEntity() = PersonEntity(
        id = id,
        name = name,
        relationship = relationship,
        photoUri = photoUri,
        phone = phone,
        email = email,
        notes = notes,
        birthDateEpochDay = birthDate?.toEpochDay(),
        birthdayEnabled = birthdayEnabled,
        reminderStartDays = reminderStartDays,
        reminderTimeMinutes = reminderTime.hour * 60 + reminderTime.minute,
        birthdayDayAlert = birthdayDayAlert,
        createdAt = createdAt.takeIf { it != 0L } ?: System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        deletedAt = null,
        dirty = true,
    )
}
