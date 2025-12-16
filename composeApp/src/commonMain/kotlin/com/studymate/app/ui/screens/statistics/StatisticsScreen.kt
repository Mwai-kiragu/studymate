package com.studymate.app.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.data.repository.StudyRecordRepository
import com.studymate.app.data.repository.StudyStats
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

class StatisticsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val noteRepository: NoteRepository = koinInject()
        val flashcardRepository: FlashcardRepository = koinInject()
        val studyRecordRepository: StudyRecordRepository = koinInject()

        var notesCount by remember { mutableStateOf(0) }
        var flashcardsCount by remember { mutableStateOf(0) }
        var dueCardsCount by remember { mutableStateOf(0L) }
        var studyStats by remember { mutableStateOf<StudyStats?>(null) }
        var subjectCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            // Load notes
            noteRepository.getAllNotes().collect { notes ->
                notesCount = notes.size
                subjectCounts = notes.groupingBy { it.subject }.eachCount()
            }
        }

        LaunchedEffect(Unit) {
            flashcardsCount = flashcardRepository.getAllFlashcards().first().size
            dueCardsCount = studyRecordRepository.countDueCards()
            studyStats = studyRecordRepository.getStudyStats()
            isLoading = false
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Statistics",
                    onBackClick = { navigator.pop() }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Overview Cards
                    item {
                        Text(
                            text = "Overview",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Description,
                                value = notesCount.toString(),
                                label = "Notes",
                                color = MaterialTheme.colorScheme.primary
                            )
                            StatCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Style,
                                value = flashcardsCount.toString(),
                                label = "Flashcards",
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    // Study Progress
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Study Progress",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    item {
                        StudyProgressCard(
                            studiedToday = studyStats?.studiedToday ?: 0,
                            dueCards = dueCardsCount.toInt(),
                            avgEaseFactor = studyStats?.avgEaseFactor ?: 2.5
                        )
                    }

                    // Subject Distribution
                    if (subjectCounts.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Notes by Subject",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        item {
                            SubjectDistributionCard(subjectCounts = subjectCounts)
                        }
                    }

                    // Tips Section
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        TipsCard()
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyProgressCard(
    studiedToday: Int,
    dueCards: Int,
    avgEaseFactor: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProgressRow(
                icon = Icons.Default.Today,
                label = "Studied Today",
                value = studiedToday.toString(),
                subtext = "cards reviewed"
            )

            HorizontalDivider()

            ProgressRow(
                icon = Icons.Default.Schedule,
                label = "Cards Due",
                value = dueCards.toString(),
                subtext = "ready for review"
            )

            HorizontalDivider()

            ProgressRow(
                icon = Icons.Default.Psychology,
                label = "Avg. Difficulty",
                value = String.format("%.2f", avgEaseFactor),
                subtext = when {
                    avgEaseFactor >= 2.5 -> "Doing great!"
                    avgEaseFactor >= 2.0 -> "Good progress"
                    else -> "Keep practicing!"
                }
            )
        }
    }
}

@Composable
private fun ProgressRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    subtext: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SubjectDistributionCard(
    subjectCounts: Map<String, Int>
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val total = subjectCounts.values.sum()
            val sortedSubjects = subjectCounts.entries.sortedByDescending { it.value }

            sortedSubjects.forEach { (subject, count) ->
                val percentage = (count.toFloat() / total * 100).toInt()

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = subject,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "$count notes ($percentage%)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { count.toFloat() / total },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun TipsCard() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.TipsAndUpdates,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Study Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "• Review cards daily for best retention\n" +
                        "• Use 'Again' when you completely forget\n" +
                        "• 'Good' is the most common rating\n" +
                        "• Create notes before exams to generate flashcards",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}
