package com.studymate.app.ui.screens.study

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.models.Flashcard
import com.studymate.app.data.models.StudyRecord
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.StudyRecordRepository
import com.studymate.app.domain.Difficulty
import com.studymate.app.ui.components.EmptyState
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class StudySessionScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val flashcardRepository: FlashcardRepository = koinInject()
        val studyRecordRepository: StudyRecordRepository = koinInject()
        val scope = rememberCoroutineScope()

        var dueRecords by remember { mutableStateOf<List<StudyRecord>>(emptyList()) }
        var flashcardMap by remember { mutableStateOf<Map<String, Flashcard>>(emptyMap()) }
        var currentIndex by remember { mutableStateOf(0) }
        var isLoading by remember { mutableStateOf(true) }
        var isFlipped by remember { mutableStateOf(false) }
        var sessionStats by remember { mutableStateOf(SessionStats()) }
        var isSessionComplete by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            // Load all flashcards
            val allFlashcards = flashcardRepository.getAllFlashcards().first()
            flashcardMap = allFlashcards.associateBy { it.id }

            // Create records for any flashcards that don't have them
            allFlashcards.forEach { card ->
                studyRecordRepository.getOrCreateRecord(card.id)
            }

            // Get due cards
            studyRecordRepository.getDueCards().collect { records ->
                dueRecords = records.filter { flashcardMap.containsKey(it.flashcardId) }
                isLoading = false
            }
        }

        fun answerCard(difficulty: Difficulty) {
            if (currentIndex >= dueRecords.size) return

            val currentRecord = dueRecords[currentIndex]

            scope.launch {
                studyRecordRepository.recordStudySession(currentRecord.flashcardId, difficulty)

                // Update stats
                sessionStats = sessionStats.copy(
                    cardsStudied = sessionStats.cardsStudied + 1,
                    correctCount = if (difficulty.ordinal >= 2) sessionStats.correctCount + 1 else sessionStats.correctCount
                )

                // Move to next card
                isFlipped = false
                if (currentIndex < dueRecords.size - 1) {
                    currentIndex++
                } else {
                    isSessionComplete = true
                }
            }
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Study Session",
                    onBackClick = { navigator.pop() }
                )
            }
        ) { paddingValues ->
            when {
                isLoading -> LoadingIndicator(modifier = Modifier.padding(paddingValues))
                dueRecords.isEmpty() -> {
                    EmptyState(
                        message = "No cards due for review! Great job keeping up with your studies.",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        action = {
                            Button(onClick = { navigator.pop() }) {
                                Text("Back to Home")
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                isSessionComplete -> {
                    SessionCompleteScreen(
                        stats = sessionStats,
                        onFinish = { navigator.pop() },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                else -> {
                    val currentRecord = dueRecords[currentIndex]
                    val currentCard = flashcardMap[currentRecord.flashcardId]

                    if (currentCard != null) {
                        StudyCardContent(
                            card = currentCard,
                            currentIndex = currentIndex + 1,
                            totalCards = dueRecords.size,
                            isFlipped = isFlipped,
                            onFlip = { isFlipped = !isFlipped },
                            onAnswer = { difficulty -> answerCard(difficulty) },
                            modifier = Modifier.padding(paddingValues)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StudyCardContent(
    card: Flashcard,
    currentIndex: Int,
    totalCards: Int,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    onAnswer: (Difficulty) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Progress indicator
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Card $currentIndex of $totalCards",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${((currentIndex.toFloat() / totalCards) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { currentIndex.toFloat() / totalCards },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Flashcard
        FlashcardDisplay(
            question = card.question,
            answer = card.answer,
            isFlipped = isFlipped,
            onFlip = onFlip,
            modifier = Modifier.weight(1f)
        )

        // Answer buttons (only show when flipped)
        if (isFlipped) {
            DifficultyButtons(onAnswer = onAnswer)
        } else {
            Button(
                onClick = onFlip,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Show Answer")
            }
        }
    }
}

@Composable
private fun FlashcardDisplay(
    question: String,
    answer: String,
    isFlipped: Boolean,
    onFlip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        label = "flip"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onFlip)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isFlipped)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                // Question side
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(0.5f),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = question,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Tap to reveal answer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Answer side (rotated back)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp)
                            .alpha(0.5f),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = answer,
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun DifficultyButtons(
    onAnswer: (Difficulty) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "How well did you know this?",
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DifficultyButton(
                text = "Again",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(Difficulty.AGAIN) }
            )
            DifficultyButton(
                text = "Hard",
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(Difficulty.HARD) }
            )
            DifficultyButton(
                text = "Good",
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(Difficulty.GOOD) }
            )
            DifficultyButton(
                text = "Easy",
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f),
                onClick = { onAnswer(Difficulty.EASY) }
            )
        }
    }
}

@Composable
private fun DifficultyButton(
    text: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SessionCompleteScreen(
    stats: SessionStats,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Celebration,
            contentDescription = null,
            modifier = Modifier.size(96.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Session Complete!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatRow(
                    label = "Cards Studied",
                    value = stats.cardsStudied.toString()
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatRow(
                    label = "Correct Answers",
                    value = stats.correctCount.toString()
                )
                Spacer(modifier = Modifier.height(16.dp))
                StatRow(
                    label = "Accuracy",
                    value = if (stats.cardsStudied > 0) {
                        "${((stats.correctCount.toFloat() / stats.cardsStudied) * 100).toInt()}%"
                    } else "N/A"
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onFinish,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Finish")
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

private data class SessionStats(
    val cardsStudied: Int = 0,
    val correctCount: Int = 0
)
