package com.studymate.app.data.models

import kotlinx.datetime.Instant

data class Flashcard(
    val id: String,
    val noteId: String,
    val question: String,
    val answer: String,
    val createdAt: Instant
)
