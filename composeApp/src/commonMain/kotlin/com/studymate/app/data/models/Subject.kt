package com.studymate.app.data.models

import kotlinx.datetime.Instant

data class Subject(
    val id: String,
    val name: String,
    val color: String,
    val icon: String? = null,
    val createdAt: Instant,
    val noteCount: Int = 0
)

// Predefined subject colors
object SubjectColors {
    val colors = listOf(
        "#E57373", // Red
        "#F06292", // Pink
        "#BA68C8", // Purple
        "#9575CD", // Deep Purple
        "#7986CB", // Indigo
        "#64B5F6", // Blue
        "#4FC3F7", // Light Blue
        "#4DD0E1", // Cyan
        "#4DB6AC", // Teal
        "#81C784", // Green
        "#AED581", // Light Green
        "#DCE775", // Lime
        "#FFF176", // Yellow
        "#FFD54F", // Amber
        "#FFB74D", // Orange
        "#FF8A65", // Deep Orange
    )

    fun getRandomColor(): String = colors.random()
}

// Predefined subject icons (Material icon names)
object SubjectIcons {
    val icons = listOf(
        "school" to "School",
        "science" to "Science",
        "calculate" to "Math",
        "history_edu" to "History",
        "menu_book" to "Literature",
        "language" to "Languages",
        "computer" to "Programming",
        "psychology" to "Psychology",
        "biotech" to "Biology",
        "architecture" to "Architecture",
        "music_note" to "Music",
        "palette" to "Art",
        "sports_soccer" to "Sports",
        "public" to "Geography",
        "gavel" to "Law",
        "business" to "Business",
        "medical_services" to "Medicine",
        "engineering" to "Engineering",
    )
}
