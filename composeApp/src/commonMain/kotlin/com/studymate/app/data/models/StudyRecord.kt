package com.studymate.app.data.models

import kotlinx.datetime.Instant

data class StudyRecord(
    val id: String,
    val flashcardId: String,
    val nextReview: Instant,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Double = 2.5,
    val lastDifficulty: String? = null,
    val lastStudiedAt: Instant? = null
)
