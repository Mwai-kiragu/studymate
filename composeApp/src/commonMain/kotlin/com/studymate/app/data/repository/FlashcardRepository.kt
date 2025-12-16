package com.studymate.app.data.repository

import com.benasher44.uuid.uuid4
import com.studymate.app.data.models.Flashcard
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

class FlashcardRepository(private val database: StudyMateDatabase) {

    private val queries = database.flashcardsQueries

    fun getAllFlashcards(): Flow<List<Flashcard>> {
        return queries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { cards -> cards.map { it.toFlashcard() } }
    }

    fun getFlashcardById(id: String): Flow<Flashcard?> {
        return queries.selectById(id)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toFlashcard() }
    }

    fun getFlashcardsByNoteId(noteId: String): Flow<List<Flashcard>> {
        return queries.selectByNoteId(noteId)
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { cards -> cards.map { it.toFlashcard() } }
    }

    suspend fun insertFlashcard(
        noteId: String,
        question: String,
        answer: String
    ): String = withContext(Dispatchers.IO) {
        val id = uuid4().toString()
        val now = Clock.System.now().toEpochMilliseconds()

        queries.insertFlashcard(
            id = id,
            noteId = noteId,
            question = question,
            answer = answer,
            createdAt = now
        )
        id
    }

    suspend fun insertFlashcards(
        noteId: String,
        cards: List<Pair<String, String>>
    ): List<String> = withContext(Dispatchers.IO) {
        val ids = mutableListOf<String>()
        val now = Clock.System.now().toEpochMilliseconds()

        cards.forEach { (question, answer) ->
            val id = uuid4().toString()
            queries.insertFlashcard(
                id = id,
                noteId = noteId,
                question = question,
                answer = answer,
                createdAt = now
            )
            ids.add(id)
        }
        ids
    }

    suspend fun updateFlashcard(
        id: String,
        question: String,
        answer: String
    ) = withContext(Dispatchers.IO) {
        queries.updateFlashcard(
            question = question,
            answer = answer,
            id = id
        )
    }

    suspend fun deleteFlashcard(id: String) = withContext(Dispatchers.IO) {
        queries.deleteFlashcard(id)
    }

    suspend fun deleteFlashcardsByNoteId(noteId: String) = withContext(Dispatchers.IO) {
        queries.deleteByNoteId(noteId)
    }

    private fun com.studymate.database.Flashcard.toFlashcard(): Flashcard {
        return Flashcard(
            id = id,
            noteId = noteId,
            question = question,
            answer = answer,
            createdAt = Instant.fromEpochMilliseconds(createdAt)
        )
    }
}
