package com.studymate.app.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.models.Note
import com.studymate.app.data.models.Subject
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.data.repository.SubjectRepository
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class NoteEditorScreen(val noteId: String?) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val noteRepository: NoteRepository = koinInject()
        val subjectRepository: SubjectRepository = koinInject()
        val scope = rememberCoroutineScope()

        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        var subject by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(noteId != null) }
        var isSaving by remember { mutableStateOf(false) }
        var showSubjectDropdown by remember { mutableStateOf(false) }
        var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }

        // Load subjects from database
        LaunchedEffect(Unit) {
            subjectRepository.ensureDefaultSubjectsExist()
            subjectRepository.getAllSubjects().collect { loadedSubjects ->
                subjects = loadedSubjects
            }
        }

        // Load existing note if editing
        LaunchedEffect(noteId) {
            if (noteId != null) {
                noteRepository.getNoteById(noteId).firstOrNull()?.let { note ->
                    title = note.title
                    content = note.content
                    subject = note.subject
                }
                isLoading = false
            }
        }

        fun saveNote() {
            if (title.isBlank() || content.isBlank() || subject.isBlank()) return

            scope.launch {
                isSaving = true
                if (noteId != null) {
                    noteRepository.updateNote(
                        id = noteId,
                        title = title,
                        content = content,
                        subject = subject
                    )
                } else {
                    noteRepository.insertNote(
                        title = title,
                        content = content,
                        subject = subject
                    )
                }
                isSaving = false
                navigator.pop()
            }
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = if (noteId != null) "Edit Note" else "New Note",
                    onBackClick = { navigator.pop() },
                    actions = {
                        IconButton(
                            onClick = { saveNote() },
                            enabled = title.isNotBlank() && content.isNotBlank() && subject.isNotBlank() && !isSaving
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                            }
                        }
                    }
                )
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        placeholder = { Text("Enter note title") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Subject Dropdown
                    ExposedDropdownMenuBox(
                        expanded = showSubjectDropdown,
                        onExpandedChange = { showSubjectDropdown = it }
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = { subject = it },
                            label = { Text("Subject") },
                            placeholder = { Text("Select or type subject") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showSubjectDropdown)
                            },
                            singleLine = true
                        )

                        ExposedDropdownMenu(
                            expanded = showSubjectDropdown,
                            onDismissRequest = { showSubjectDropdown = false }
                        ) {
                            subjects.forEach { subjectOption ->
                                DropdownMenuItem(
                                    text = { Text(subjectOption.name) },
                                    onClick = {
                                        subject = subjectOption.name
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                            // Option to add new subject
                            if (subject.isNotBlank() && subjects.none { it.name.equals(subject, ignoreCase = true) }) {
                                HorizontalDivider()
                                DropdownMenuItem(
                                    text = {
                                        Row {
                                            Icon(
                                                Icons.Default.Add,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Add \"$subject\" as new subject")
                                        }
                                    },
                                    onClick = {
                                        scope.launch {
                                            subjectRepository.insertSubject(subject)
                                        }
                                        showSubjectDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Content
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("Content") },
                        placeholder = { Text("Write your study notes here...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 300.dp),
                        maxLines = Int.MAX_VALUE
                    )

                    // Tips Card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tips for better notes",
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Include key definitions and concepts\n" +
                                        "• Use bullet points for organization\n" +
                                        "• Add examples to clarify ideas\n" +
                                        "• After saving, use AI to generate flashcards!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
