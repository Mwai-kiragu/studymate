package com.studymate.app.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.studymate.app.ai.GeminiService
import com.studymate.app.data.models.Note
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.data.repository.StudyRecordRepository
import com.studymate.app.ui.components.ConfirmationDialog
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

data class NoteDetailScreen(val noteId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val noteRepository: NoteRepository = koinInject()
        val flashcardRepository: FlashcardRepository = koinInject()
        val studyRecordRepository: StudyRecordRepository = koinInject()
        val geminiService: GeminiService = koinInject()
        val scope = rememberCoroutineScope()

        var note by remember { mutableStateOf<Note?>(null) }
        var flashcardCount by remember { mutableStateOf(0) }
        var isLoading by remember { mutableStateOf(true) }
        var showDeleteDialog by remember { mutableStateOf(false) }
        var isGeneratingSummary by remember { mutableStateOf(false) }
        var isGeneratingFlashcards by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf<String?>(null) }
        var successMessage by remember { mutableStateOf<String?>(null) }
        var showShareDialog by remember { mutableStateOf(false) }

        LaunchedEffect(noteId) {
            noteRepository.getNoteById(noteId).collect { loadedNote ->
                note = loadedNote
                isLoading = false
            }
        }

        LaunchedEffect(noteId) {
            flashcardRepository.getFlashcardsByNoteId(noteId).collect { cards ->
                flashcardCount = cards.size
            }
        }

        // Clear messages after showing
        LaunchedEffect(successMessage) {
            if (successMessage != null) {
                kotlinx.coroutines.delay(3000)
                successMessage = null
            }
        }

        fun generateSummary() {
            note?.let { currentNote ->
                scope.launch {
                    isGeneratingSummary = true
                    errorMessage = null
                    val result = geminiService.summarizeNotes(currentNote.content)
                    result.fold(
                        onSuccess = { summary ->
                            noteRepository.updateNote(
                                id = currentNote.id,
                                title = currentNote.title,
                                content = currentNote.content,
                                subject = currentNote.subject,
                                summary = summary
                            )
                            successMessage = "Summary generated!"
                        },
                        onFailure = { error ->
                            errorMessage = error.message ?: "Failed to generate summary"
                        }
                    )
                    isGeneratingSummary = false
                }
            }
        }

        fun generateFlashcards() {
            note?.let { currentNote ->
                scope.launch {
                    isGeneratingFlashcards = true
                    errorMessage = null
                    val result = geminiService.generateFlashcards(currentNote.content, 5)
                    result.fold(
                        onSuccess = { flashcards ->
                            val cardPairs = flashcards.map { it.question to it.answer }
                            val cardIds = flashcardRepository.insertFlashcards(noteId, cardPairs)
                            // Create study records for new cards
                            cardIds.forEach { cardId ->
                                studyRecordRepository.getOrCreateRecord(cardId)
                            }
                            successMessage = "${flashcards.size} flashcards generated!"
                        },
                        onFailure = { error ->
                            errorMessage = error.message ?: "Failed to generate flashcards"
                        }
                    )
                    isGeneratingFlashcards = false
                }
            }
        }

        fun deleteNote() {
            scope.launch {
                noteRepository.deleteNote(noteId)
                navigator.pop()
            }
        }

        if (showDeleteDialog) {
            ConfirmationDialog(
                title = "Delete Note",
                message = "Are you sure you want to delete this note? This will also delete all associated flashcards.",
                confirmText = "Delete",
                onConfirm = {
                    showDeleteDialog = false
                    deleteNote()
                },
                onDismiss = { showDeleteDialog = false }
            )
        }

        if (showShareDialog) {
            ShareNoteDialog(
                note = note!!,
                onShare = { tags, authorName ->
                    scope.launch {
                        noteRepository.shareNote(
                            noteId = noteId,
                            isPublic = true,
                            tags = tags,
                            authorName = authorName
                        )
                        successMessage = "Note shared to community!"
                    }
                    showShareDialog = false
                },
                onUnshare = {
                    scope.launch {
                        noteRepository.shareNote(
                            noteId = noteId,
                            isPublic = false
                        )
                        successMessage = "Note removed from community"
                    }
                    showShareDialog = false
                },
                onDismiss = { showShareDialog = false }
            )
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Note Details",
                    onBackClick = { navigator.pop() },
                    actions = {
                        IconButton(onClick = { showShareDialog = true }) {
                            Icon(
                                if (note?.isPublic == true) Icons.Default.Public else Icons.Default.Share,
                                contentDescription = "Share"
                            )
                        }
                        IconButton(onClick = { navigator.push(NoteEditorScreen(noteId = noteId)) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                )
            }
        ) { paddingValues ->
            when {
                isLoading -> LoadingIndicator(modifier = Modifier.padding(paddingValues))
                note == null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Note not found")
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Error/Success Messages
                        errorMessage?.let { error ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = error,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }

                        successMessage?.let { success ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Text(
                                    text = success,
                                    modifier = Modifier.padding(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }

                        // Note Header
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = note!!.title,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(note!!.subject) }
                                )
                            }

                            Text(
                                text = "Updated: ${formatDate(note!!.updatedAt)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (flashcardCount > 0) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$flashcardCount flashcards",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider()

                        // AI Actions
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { generateSummary() },
                                enabled = !isGeneratingSummary && !isGeneratingFlashcards,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isGeneratingSummary) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (note!!.summary != null) "Regenerate" else "Summarize")
                            }

                            Button(
                                onClick = { generateFlashcards() },
                                enabled = !isGeneratingSummary && !isGeneratingFlashcards,
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isGeneratingFlashcards) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.Style,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Generate Cards")
                            }
                        }

                        // AI Summary (if available)
                        note!!.summary?.let { summary ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "AI Summary",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = summary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        // Note Content
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Content",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note!!.content,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

private fun formatDate(instant: kotlinx.datetime.Instant): String {
    val localDateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${localDateTime.month.name.take(3)} ${localDateTime.dayOfMonth}, ${localDateTime.year} at ${localDateTime.hour}:${localDateTime.minute.toString().padStart(2, '0')}"
}
