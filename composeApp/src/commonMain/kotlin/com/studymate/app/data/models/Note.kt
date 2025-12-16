package com.studymate.app.data.models

import kotlinx.datetime.Instant

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val subject: String,
    val createdAt: Instant,
    val updatedAt: Instant,
    val summary: String? = null,
    val imageUrls: List<String> = emptyList(),
    val isPublic: Boolean = false,
    val tags: List<String> = emptyList(),
    val sharedAt: Instant? = null,
    val authorName: String? = null,
    val downloadCount: Int = 0
)
