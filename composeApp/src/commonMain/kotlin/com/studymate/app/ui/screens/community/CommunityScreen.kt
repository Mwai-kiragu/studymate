package com.studymate.app.ui.screens.community

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.studymate.app.data.models.Note
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.ui.components.EmptyState
import com.studymate.app.ui.components.LoadingIndicator
import com.studymate.app.ui.components.StudyMateTopBar
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

class CommunityScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val noteRepository: NoteRepository = koinInject()
        val scope = rememberCoroutineScope()

        var publicNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
        var popularNotes by remember { mutableStateOf<List<Note>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedSubject by remember { mutableStateOf<String?>(null) }
        var showImportDialog by remember { mutableStateOf<Note?>(null) }

        val subjects = listOf("Mathematics", "Science", "History", "Literature", "Programming", "Languages", "Other")

        LaunchedEffect(Unit) {
            noteRepository.getPublicNotes().collect { notes ->
                publicNotes = notes
                isLoading = false
            }
        }

        LaunchedEffect(Unit) {
            noteRepository.getPopularNotes(10).collect { notes ->
                popularNotes = notes
            }
        }

        LaunchedEffect(searchQuery, selectedSubject) {
            if (searchQuery.isNotEmpty()) {
                noteRepository.searchPublicNotes(searchQuery).collect { notes ->
                    publicNotes = notes
                }
            } else if (selectedSubject != null) {
                noteRepository.getPublicNotesBySubject(selectedSubject!!).collect { notes ->
                    publicNotes = notes
                }
            } else {
                noteRepository.getPublicNotes().collect { notes ->
                    publicNotes = notes
                }
            }
        }

        showImportDialog?.let { note ->
            AlertDialog(
                onDismissRequest = { showImportDialog = null },
                title = { Text("Import Note") },
                text = {
                    Column {
                        Text("Do you want to import this note to your collection?")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "\"${note.title}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        if (note.authorName != null) {
                            Text(
                                text = "by ${note.authorName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                noteRepository.importNote(note)
                                noteRepository.incrementDownloadCount(note.id)
                            }
                            showImportDialog = null
                        }
                    ) {
                        Text("Import")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Scaffold(
            topBar = {
                StudyMateTopBar(
                    title = "Community Notes",
                    onBackClick = { navigator.pop() }
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search community notes...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true
                )

                // Subject filter chips
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedSubject == null,
                            onClick = { selectedSubject = null },
                            label = { Text("All") }
                        )
                    }
                    items(subjects) { subject ->
                        FilterChip(
                            selected = selectedSubject == subject,
                            onClick = {
                                selectedSubject = if (selectedSubject == subject) null else subject
                            },
                            label = { Text(subject) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                when {
                    isLoading -> LoadingIndicator()
                    publicNotes.isEmpty() && popularNotes.isEmpty() -> {
                        EmptyState(
                            message = "No shared notes yet. Be the first to share your knowledge!",
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.Groups,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Popular notes section
                            if (popularNotes.isNotEmpty() && searchQuery.isEmpty() && selectedSubject == null) {
                                item {
                                    Text(
                                        text = "🔥 Popular Notes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                item {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(popularNotes, key = { "popular_${it.id}" }) { note ->
                                            PopularNoteCard(
                                                note = note,
                                                onImport = { showImportDialog = note },
                                                onClick = { navigator.push(CommunityNoteDetailScreen(note)) }
                                            )
                                        }
                                    }
                                }

                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "📚 All Shared Notes",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Results count
                            item {
                                Text(
                                    text = "${publicNotes.size} notes available",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            items(publicNotes, key = { it.id }) { note ->
                                CommunityNoteCard(
                                    note = note,
                                    onImport = { showImportDialog = note },
                                    onClick = { navigator.push(CommunityNoteDetailScreen(note)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PopularNoteCard(
    note: Note,
    onImport: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = note.subject,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )

            if (note.authorName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "by ${note.authorName}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${note.downloadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onImport,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CommunityNoteCard(
    note: Note,
    onImport: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AssistChip(
                            onClick = { },
                            label = { Text(note.subject) },
                            modifier = Modifier.height(24.dp)
                        )

                        if (note.authorName != null) {
                            Text(
                                text = "by ${note.authorName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                IconButton(onClick = onImport) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Import",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (note.summary != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(note.tags.take(5)) { tag ->
                        SuggestionChip(
                            onClick = { },
                            label = { Text("#$tag", style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${note.downloadCount} imports",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(onClick = onClick) {
                    Text("View")
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
