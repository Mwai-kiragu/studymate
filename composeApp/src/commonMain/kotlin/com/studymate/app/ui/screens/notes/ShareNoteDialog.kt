package com.studymate.app.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studymate.app.data.models.Note

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareNoteDialog(
    note: Note,
    onShare: (tags: List<String>, authorName: String?) -> Unit,
    onUnshare: () -> Unit,
    onDismiss: () -> Unit
) {
    var authorName by remember { mutableStateOf(note.authorName ?: "") }
    var tagsText by remember { mutableStateOf(note.tags.joinToString(", ")) }
    var showUnshareConfirm by remember { mutableStateOf(false) }

    val suggestedTags = listOf("study", "notes", "exam", "summary", "tutorial", "guide", note.subject.lowercase())

    if (showUnshareConfirm) {
        AlertDialog(
            onDismissRequest = { showUnshareConfirm = false },
            title = { Text("Remove from Community?") },
            text = { Text("This note will no longer be visible to other students.") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnshareConfirm = false
                        onUnshare()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnshareConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (note.isPublic) Icons.Default.Public else Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(if (note.isPublic) "Shared Note" else "Share to Community")
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (note.isPublic) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "This note is shared with the community",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Share your knowledge with other students! Your note will be visible in the Community section.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                OutlinedTextField(
                    value = authorName,
                    onValueChange = { authorName = it },
                    label = { Text("Your name (optional)") },
                    placeholder = { Text("Anonymous") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    }
                )

                Column {
                    OutlinedTextField(
                        value = tagsText,
                        onValueChange = { tagsText = it },
                        label = { Text("Tags (comma separated)") },
                        placeholder = { Text("e.g., math, algebra, exam") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = {
                            Icon(Icons.Default.Tag, contentDescription = null)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Suggested tags:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(suggestedTags.distinct()) { tag ->
                            SuggestionChip(
                                onClick = {
                                    val currentTags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                    if (!currentTags.contains(tag)) {
                                        tagsText = if (tagsText.isEmpty()) tag else "$tagsText, $tag"
                                    }
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (note.isPublic) {
                    HorizontalDivider()

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Remove from Community",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Make this note private again",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showUnshareConfirm = true }) {
                            Icon(
                                Icons.Default.RemoveCircle,
                                contentDescription = "Unshare",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val tags = tagsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onShare(tags, authorName.ifEmpty { null })
                }
            ) {
                Icon(
                    if (note.isPublic) Icons.Default.Save else Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (note.isPublic) "Update" else "Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
