package com.studymate.app.data.repository

import com.benasher44.uuid.uuid4
import com.studymate.app.data.models.Subject
import com.studymate.app.data.models.SubjectColors
import com.studymate.database.StudyMateDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull

class SubjectRepository(private val database: StudyMateDatabase) {

    private val queries = database.subjectsQueries

    fun getAllSubjects(): Flow<List<Subject>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { subjects -> subjects.map { it.toSubject() } }
    }

    fun getSubjectById(id: String): Flow<Subject?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toSubject() }
    }

    fun getSubjectByName(name: String): Flow<Subject?> {
        return queries.selectByName(name)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toSubject() }
    }

    fun searchSubjects(query: String): Flow<List<Subject>> {
        return queries.searchSubjects(query, query)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { subjects -> subjects.map { it.toSubject() } }
    }

    suspend fun insertSubject(
        name: String,
        color: String = SubjectColors.getRandomColor(),
        icon: String? = null
    ): String = withContext(Dispatchers.IO) {
        val id = uuid4().toString()
        val now = Clock.System.now().toEpochMilliseconds()

        queries.insertSubject(
            id = id,
            name = name,
            color = color,
            icon = icon,
            createdAt = now,
            noteCount = 0L
        )
        id
    }

    suspend fun updateSubject(
        id: String,
        name: String,
        color: String,
        icon: String? = null
    ) = withContext(Dispatchers.IO) {
        queries.updateSubject(
            name = name,
            color = color,
            icon = icon,
            id = id
        )
    }

    suspend fun deleteSubject(id: String) = withContext(Dispatchers.IO) {
        queries.deleteSubject(id)
    }

    suspend fun refreshNoteCount(subjectId: String) = withContext(Dispatchers.IO) {
        queries.updateNoteCount(subjectId, subjectId)
    }

    suspend fun ensureDefaultSubjectsExist() = withContext(Dispatchers.IO) {
        val defaultSubjects = listOf(
            Triple("Mathematics", "#64B5F6", "calculate"),
            Triple("Science", "#81C784", "science"),
            Triple("History", "#FFB74D", "history_edu"),
            Triple("Literature", "#BA68C8", "menu_book"),
            Triple("Programming", "#4DD0E1", "computer"),
            Triple("Languages", "#F06292", "language"),
            Triple("Other", "#9E9E9E", "school")
        )

        defaultSubjects.forEach { (name, color, icon) ->
            val existing = queries.selectByName(name).executeAsOneOrNull()
            if (existing == null) {
                insertSubject(name, color, icon)
            }
        }
    }

    private fun com.studymate.database.Subject.toSubject(): Subject {
        return Subject(
            id = id,
            name = name,
            color = color,
            icon = icon,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            noteCount = noteCount.toInt()
        )
    }
}
