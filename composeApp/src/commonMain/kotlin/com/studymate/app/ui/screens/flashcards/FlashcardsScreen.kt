package com.studymate.app.ui.screens.flashcards

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.models.Flashcard
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.ui.components.ConfirmationDialog
import com.studymate.app.ui.components.EmptyState
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import com.studymate.app.ui.screens.study.StudySessionScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class FlashcardsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val flashcardRepository: FlashcardRepository = koinInject()
        val noteRepository: NoteRepository = koinInject()
        val scope = rememberCoroutineScope()

        var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }
        var noteNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
        var isLoading by remember { mutableStateOf(true) }
        var cardToDelete by remember { mutableStateOf<Flashcard?>(null) }

        LaunchedEffect(Unit) {
            flashcardRepository.getAllFlashcards().collect { cards ->
                flashcards = cards
                isLoading = false
            }
        }

        LaunchedEffect(Unit) {
            noteRepository.getAllNotes().collect { notes ->
                noteNames = notes.associate { it.id to it.title }
            }
        }

        cardToDelete?.let { card ->
            ConfirmationDialog(
                title = "Delete Flashcard",
                message = "Are you sure you want to delete this flashcard?",
                confirmText = "Delete",
                onConfirm = {
                    scope.launch {
                        flashcardRepository.deleteFlashcard(card.id)
                    }
                    cardToDelete = null
                },
                onDismiss = { cardToDelete = null }
            )
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Flashcards",
                    onBackClick = { navigator.pop() }
                )
            },
            floatingActionButton = {
                if (flashcards.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { navigator.push(StudySessionScreen()) },
                        icon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                        text = { Text("Study Now") }
                    )
                }
            }
        ) { paddingValues ->
            when {
                isLoading -> LoadingIndicator(modifier = Modifier.padding(paddingValues))
                flashcards.isEmpty() -> {
                    EmptyState(
                        message = "No flashcards yet. Create notes and generate flashcards with AI!",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Style,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = "${flashcards.size} flashcards",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(flashcards, key = { it.id }) { flashcard ->
                            FlashcardItem(
                                flashcard = flashcard,
                                noteName = noteNames[flashcard.noteId] ?: "Unknown",
                                onDelete = { cardToDelete = flashcard }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashcardItem(
    flashcard: Flashcard,
    noteName: String,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Q: ${flashcard.question}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "From: $noteName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "A: ${flashcard.answer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { isExpanded = !isExpanded }) {
                    Text(if (isExpanded) "Hide Answer" else "Show Answer")
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
