package com.studymate.app.data.repository

import com.benasher44.uuid.uuid4
import com.studymate.app.data.models.Note
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NoteRepository(private val database: StudyMateDatabase) {

    private val queries = database.notesQueries

    fun getAllNotes(): Flow<List<Note>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    fun getNoteById(id: String): Flow<Note?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toNote() }
    }

    fun getNotesBySubject(subject: String): Flow<List<Note>> {
        return queries.selectBySubject(subject)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    fun searchNotes(query: String): Flow<List<Note>> {
        // Pass query 9 times: 5 for WHERE conditions, 4 for ORDER BY ranking
        return queries.searchNotes(
            query, query, query, query, query,  // WHERE conditions
            query, query, query, query           // ORDER BY ranking
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    suspend fun insertNote(
        title: String,
        content: String,
        subject: String,
        summary: String? = null,
        imageUrls: List<String> = emptyList(),
        isPublic: Boolean = false,
        tags: List<String> = emptyList(),
        authorName: String? = null
    ): String = withContext(Dispatchers.IO) {
        val id = uuid4().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val imageUrlsJson = if (imageUrls.isNotEmpty()) Json.encodeToString(imageUrls) else null
        val tagsJson = if (tags.isNotEmpty()) Json.encodeToString(tags) else null
        val sharedAt = if (isPublic) now else null

        queries.insertNote(
            id = id,
            title = title,
            content = content,
            subject = subject,
            createdAt = now,
            updatedAt = now,
            summary = summary,
            imageUrls = imageUrlsJson,
            isPublic = if (isPublic) 1L else 0L,
            tags = tagsJson,
            sharedAt = sharedAt,
            authorName = authorName,
            downloadCount = 0L
        )
        id
    }

    suspend fun updateNote(
        id: String,
        title: String,
        content: String,
        subject: String,
        summary: String? = null,
        imageUrls: List<String> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        val imageUrlsJson = if (imageUrls.isNotEmpty()) Json.encodeToString(imageUrls) else null

        queries.updateNote(
            title = title,
            content = content,
            subject = subject,
            updatedAt = now,
            summary = summary,
            imageUrls = imageUrlsJson,
            id = id
        )
    }

    suspend fun deleteNote(id: String) = withContext(Dispatchers.IO) {
        queries.deleteNote(id)
    }

    // Community features
    fun getPublicNotes(): Flow<List<Note>> {
        return queries.selectPublicNotes()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    fun getPublicNotesBySubject(subject: String): Flow<List<Note>> {
        return queries.selectPublicBySubject(subject)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    fun searchPublicNotes(query: String): Flow<List<Note>> {
        // Pass query 10 times: 6 for WHERE conditions, 4 for ORDER BY ranking
        return queries.searchPublicNotes(
            query, query, query, query, query, query,  // WHERE conditions
            query, query, query, query                  // ORDER BY ranking
        )
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    fun getPopularNotes(limit: Long = 20): Flow<List<Note>> {
        return queries.selectPopularNotes(limit)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { notes -> notes.map { it.toNote() } }
    }

    suspend fun shareNote(
        noteId: String,
        isPublic: Boolean,
        tags: List<String> = emptyList(),
        authorName: String? = null
    ) = withContext(Dispatchers.IO) {
        val tagsJson = if (tags.isNotEmpty()) Json.encodeToString(tags) else null
        val sharedAt = if (isPublic) Clock.System.now().toEpochMilliseconds() else null

        queries.updateNoteSharing(
            isPublic = if (isPublic) 1L else 0L,
            tags = tagsJson,
            sharedAt = sharedAt,
            authorName = authorName,
            id = noteId
        )
    }

    suspend fun incrementDownloadCount(noteId: String) = withContext(Dispatchers.IO) {
        queries.incrementDownloadCount(noteId)
    }

    suspend fun importNote(note: Note): String = withContext(Dispatchers.IO) {
        val newId = uuid4().toString()
        val now = Clock.System.now().toEpochMilliseconds()
        val imageUrlsJson = if (note.imageUrls.isNotEmpty()) Json.encodeToString(note.imageUrls) else null

        queries.insertNote(
            id = newId,
            title = note.title,
            content = note.content,
            subject = note.subject,
            createdAt = now,
            updatedAt = now,
            summary = note.summary,
            imageUrls = imageUrlsJson,
            isPublic = 0L, // Imported notes are private by default
            tags = null,
            sharedAt = null,
            authorName = null,
            downloadCount = 0L
        )
        newId
    }

    private fun com.studymate.database.Note.toNote(): Note {
        val imageUrlsList: List<String> = imageUrls?.let {
            try {
                Json.decodeFromString<List<String>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        val tagsList: List<String> = tags?.let {
            try {
                Json.decodeFromString<List<String>>(it)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()

        return Note(
            id = id,
            title = title,
            content = content,
            subject = subject,
            createdAt = Instant.fromEpochMilliseconds(createdAt),
            updatedAt = Instant.fromEpochMilliseconds(updatedAt),
            summary = summary,
            imageUrls = imageUrlsList,
            isPublic = isPublic == 1L,
            tags = tagsList,
            sharedAt = sharedAt?.let { Instant.fromEpochMilliseconds(it) },
            authorName = authorName,
            downloadCount = downloadCount.toInt()
        )
    }
}
