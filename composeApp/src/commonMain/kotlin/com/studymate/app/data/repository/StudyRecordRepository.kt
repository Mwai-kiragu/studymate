package com.studymate.app.data.repository

import com.benasher44.uuid.uuid4
import com.studymate.app.data.models.StudyRecord
import com.studymate.app.domain.CardSchedule
import com.studymate.app.domain.Difficulty
import com.studymate.app.domain.SM2Algorithm
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

class StudyRecordRepository(private val database: StudyMateDatabase) {

    private val queries = database.study_recordsQueries

    fun getDueCards(currentTime: Instant = Clock.System.now()): Flow<List<StudyRecord>> {
        return queries.selectDueCards(currentTime.toEpochMilliseconds())
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { records -> records.map { it.toStudyRecord() } }
    }

    fun getRecordByFlashcardId(flashcardId: String): Flow<StudyRecord?> {
        return queries.selectByFlashcardId(flashcardId)
            .asFlow()
            .mapToOneOrNull(Dispatchers.IO)
            .map { it?.toStudyRecord() }
    }

    suspend fun countDueCards(currentTime: Instant = Clock.System.now()): Long = withContext(Dispatchers.IO) {
        queries.countDueCards(currentTime.toEpochMilliseconds()).executeAsOne()
    }

    suspend fun getOrCreateRecord(flashcardId: String): StudyRecord = withContext(Dispatchers.IO) {
        val existing = queries.selectByFlashcardId(flashcardId).executeAsOneOrNull()
        if (existing != null) {
            existing.toStudyRecord()
        } else {
            val id = uuid4().toString()
            val now = Clock.System.now()
            queries.insertRecord(
                id = id,
                flashcardId = flashcardId,
                nextReview = now.toEpochMilliseconds(),
                interval = 0,
                repetitions = 0,
                easeFactor = 2.5,
                lastDifficulty = null,
                lastStudiedAt = null
            )
            StudyRecord(
                id = id,
                flashcardId = flashcardId,
                nextReview = now,
                interval = 0,
                repetitions = 0,
                easeFactor = 2.5,
                lastDifficulty = null,
                lastStudiedAt = null
            )
        }
    }

    suspend fun recordStudySession(
        flashcardId: String,
        difficulty: Difficulty
    ): StudyRecord = withContext(Dispatchers.IO) {
        val record = getOrCreateRecord(flashcardId)
        val currentSchedule = CardSchedule(
            nextReviewDate = record.nextReview,
            interval = record.interval,
            repetitions = record.repetitions,
            easeFactor = record.easeFactor
        )

        val newSchedule = SM2Algorithm.calculateNextReview(difficulty, currentSchedule)
        val now = Clock.System.now()

        queries.updateRecord(
            nextReview = newSchedule.nextReviewDate.toEpochMilliseconds(),
            interval = newSchedule.interval.toLong(),
            repetitions = newSchedule.repetitions.toLong(),
            easeFactor = newSchedule.easeFactor,
            lastDifficulty = difficulty.name,
            lastStudiedAt = now.toEpochMilliseconds(),
            id = record.id
        )

        record.copy(
            nextReview = newSchedule.nextReviewDate,
            interval = newSchedule.interval,
            repetitions = newSchedule.repetitions,
            easeFactor = newSchedule.easeFactor,
            lastDifficulty = difficulty.name,
            lastStudiedAt = now
        )
    }

    suspend fun getStudyStats(): StudyStats = withContext(Dispatchers.IO) {
        val todayStart = Clock.System.now().toEpochMilliseconds() - (24 * 60 * 60 * 1000)
        val stats = queries.getStudyStats(todayStart).executeAsOne()
        StudyStats(
            totalCards = stats.COUNT ?: 0,
            studiedToday = stats.SUM?.toInt() ?: 0,
            avgEaseFactor = stats.AVG ?: 2.5
        )
    }

    private fun com.studymate.database.StudyRecord.toStudyRecord(): StudyRecord {
        return StudyRecord(
            id = id,
            flashcardId = flashcardId,
            nextReview = Instant.fromEpochMilliseconds(nextReview),
            interval = interval.toInt(),
            repetitions = repetitions.toInt(),
            easeFactor = easeFactor,
            lastDifficulty = lastDifficulty,
            lastStudiedAt = lastStudiedAt?.let { Instant.fromEpochMilliseconds(it) }
        )
    }
}

data class StudyStats(
    val totalCards: Long,
    val studiedToday: Int,
    val avgEaseFactor: Double
)
