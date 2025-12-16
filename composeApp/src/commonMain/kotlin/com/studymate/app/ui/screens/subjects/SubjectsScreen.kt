package com.studymate.app.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.models.Subject
import com.studymate.app.data.models.SubjectColors
import com.studymate.app.data.repository.SubjectRepository
import com.studymate.app.ui.components.ConfirmationDialog
import com.studymate.app.ui.components.EmptyState
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class SubjectsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val subjectRepository: SubjectRepository = koinInject()
        val scope = rememberCoroutineScope()

        var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var showAddDialog by remember { mutableStateOf(false) }
        var subjectToEdit by remember { mutableStateOf<Subject?>(null) }
        var subjectToDelete by remember { mutableStateOf<Subject?>(null) }

        // Initialize default subjects on first load
        LaunchedEffect(Unit) {
            subjectRepository.ensureDefaultSubjectsExist()
        }

        LaunchedEffect(Unit) {
            subjectRepository.getAllSubjects().collect { loadedSubjects ->
                subjects = loadedSubjects
                isLoading = false
            }
        }

        // Add/Edit Dialog
        if (showAddDialog || subjectToEdit != null) {
            AddEditSubjectDialog(
                subject = subjectToEdit,
                onSave = { name, color ->
                    scope.launch {
                        if (subjectToEdit != null) {
                            subjectRepository.updateSubject(
                                id = subjectToEdit!!.id,
                                name = name,
                                color = color
                            )
                        } else {
                            subjectRepository.insertSubject(name, color)
                        }
                    }
                    showAddDialog = false
                    subjectToEdit = null
                },
                onDismiss = {
                    showAddDialog = false
                    subjectToEdit = null
                }
            )
        }

        // Delete Confirmation
        subjectToDelete?.let { subject ->
            ConfirmationDialog(
                title = "Delete Subject",
                message = "Are you sure you want to delete \"${subject.name}\"? Notes with this subject will keep their current subject name.",
                confirmText = "Delete",
                onConfirm = {
                    scope.launch {
                        subjectRepository.deleteSubject(subject.id)
                    }
                    subjectToDelete = null
                },
                onDismiss = { subjectToDelete = null }
            )
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Subjects",
                    onBackClick = { navigator.pop() }
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showAddDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add Subject") }
                )
            }
        ) { paddingValues ->
            when {
                isLoading -> LoadingIndicator(modifier = Modifier.padding(paddingValues))
                subjects.isEmpty() -> {
                    EmptyState(
                        message = "No subjects yet. Add your first subject!",
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Category,
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
                                text = "${subjects.size} subjects",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        items(subjects, key = { it.id }) { subject ->
                            SubjectCard(
                                subject = subject,
                                onEdit = { subjectToEdit = subject },
                                onDelete = { subjectToDelete = subject }
                            )
                        }

                        // Bottom spacing for FAB
                        item {
                            Spacer(modifier = Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val colorString = hex.removePrefix("#")
        Color(colorString.toLong(16) or 0xFF000000)
    } catch (e: Exception) {
        Color(0xFF6200EE) // Default purple
    }
}

@Composable
private fun SubjectCard(
    subject: Subject,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val subjectColor = parseColor(subject.color)

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(subjectColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = subject.name.take(2).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${subject.noteCount} notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Edit",
                    tint = MaterialTheme.colorScheme.primary
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditSubjectDialog(
    subject: Subject?,
    onSave: (name: String, color: String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var selectedColor by remember { mutableStateOf(subject?.color ?: SubjectColors.getRandomColor()) }
    var nameError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (subject != null) "Edit Subject" else "Add Subject")
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = null
                    },
                    label = { Text("Subject Name") },
                    placeholder = { Text("e.g., Mathematics") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { { Text(it) } },
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Choose a color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(SubjectColors.colors) { color ->
                        val colorValue = parseColor(color)

                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colorValue)
                                .then(
                                    if (selectedColor == color) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColor = color },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == color) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // Preview
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val previewColor = parseColor(selectedColor)

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(previewColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(2).uppercase().ifEmpty { "?" },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = name.ifEmpty { "Preview" },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        nameError = "Subject name is required"
                    } else {
                        onSave(name.trim(), selectedColor)
                    }
                }
            ) {
                Text(if (subject != null) "Save" else "Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
