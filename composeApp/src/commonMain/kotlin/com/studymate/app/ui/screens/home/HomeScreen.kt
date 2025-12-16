package com.studymate.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.studymate.app.data.models.Flashcard
import com.studymate.app.data.models.Note
import com.studymate.app.data.models.Subject
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.data.repository.StudyRecordRepository
import com.studymate.app.data.repository.SubjectRepository
import com.studymate.app.ui.screens.community.CommunityScreen
import com.studymate.app.ui.screens.flashcards.FlashcardsScreen
import com.studymate.app.ui.screens.notes.NoteEditorScreen
import com.studymate.app.ui.screens.notes.NotesListScreen
import com.studymate.app.ui.screens.statistics.StatisticsScreen
import com.studymate.app.ui.screens.study.StudySessionScreen
import com.studymate.app.ui.screens.subjects.SubjectsScreen
import com.studymate.app.ui.theme.*
import kotlinx.datetime.*
import org.koin.compose.koinInject

class HomeScreen : Screen {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        var selectedNavItem by remember { mutableStateOf(0) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                StudyBuddyBottomNav(
                    selectedItem = selectedNavItem,
                    onItemSelected = { index ->
                        selectedNavItem = index
                    }
                )
            }
        ) { paddingValues ->
            // Display content based on selected nav item
            when (selectedNavItem) {
                0 -> DashboardContent(
                    paddingValues = paddingValues,
                    navigator = navigator
                )
                1 -> NotesTabContent(paddingValues = paddingValues, navigator = navigator)
                2 -> FlashcardsTabContent(paddingValues = paddingValues, navigator = navigator)
                3 -> CommunityTabContent(paddingValues = paddingValues)
                4 -> StatisticsTabContent(paddingValues = paddingValues)
                5 -> SubjectsTabContent(paddingValues = paddingValues)
            }
        }
    }
}

// ============== TAB CONTENT COMPOSABLES ==============

@Composable
private fun DashboardContent(
    paddingValues: PaddingValues,
    navigator: Navigator
) {
    val noteRepository: NoteRepository = koinInject()
    val flashcardRepository: FlashcardRepository = koinInject()
    val studyRecordRepository: StudyRecordRepository = koinInject()

    var notesCount by remember { mutableStateOf(0) }
    var flashcardsCount by remember { mutableStateOf(0) }
    var dueCardsCount by remember { mutableStateOf(0L) }

    // Get current date
    val currentDate = remember {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dayOfWeek = now.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        val month = now.month.name.lowercase().replaceFirstChar { it.uppercase() }
        "$dayOfWeek, ${month.take(3)} ${now.dayOfMonth}"
    }

    LaunchedEffect(Unit) {
        noteRepository.getAllNotes().collect { notes ->
            notesCount = notes.size
        }
    }

    LaunchedEffect(Unit) {
        flashcardRepository.getAllFlashcards().collect { flashcards ->
            flashcardsCount = flashcards.size
        }
    }

    LaunchedEffect(Unit) {
        dueCardsCount = studyRecordRepository.countDueCards()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Dashboard",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Greeting and Add Task button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Hello, Student!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = currentDate,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { navigator.push(NoteEditorScreen(noteId = null)) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Note", fontWeight = FontWeight.Medium)
                }
            }
        }

        // Stats Cards Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.CheckBox,
                    label = "Notes Created",
                    value = "$notesCount",
                    progress = (notesCount.coerceAtMost(20) / 20f),
                    progressColor = ProgressBlue
                )
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.AccessTime,
                    label = "Flashcards",
                    value = "$flashcardsCount",
                    progress = (flashcardsCount.coerceAtMost(50) / 50f),
                    progressColor = ProgressPurple
                )
                StatsCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.AutoMirrored.Outlined.MenuBook,
                    label = "Due Cards",
                    value = "$dueCardsCount",
                    progress = null,
                    progressColor = ProgressGreen
                )
            }
        }

        // Motivational Quote
        item {
            QuoteCard(
                quote = "The beautiful thing about learning is that no one can take it away from you."
            )
        }

        // Study Now Card (if cards are due)
        if (dueCardsCount > 0) {
            item {
                StudyNowCard(
                    dueCount = dueCardsCount.toInt(),
                    onClick = { navigator.push(StudySessionScreen()) }
                )
            }
        }

        // Quick Actions Section
        item {
            SectionHeader(
                title = "Quick Actions",
                onSeeAll = null
            )
        }

        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(getQuickActions()) { action ->
                    QuickActionCard(
                        action = action,
                        onClick = {
                            when (action.id) {
                                "notes" -> navigator.push(NotesListScreen())
                                "flashcards" -> navigator.push(FlashcardsScreen())
                                "study" -> navigator.push(StudySessionScreen())
                                "community" -> navigator.push(CommunityScreen())
                                "subjects" -> navigator.push(SubjectsScreen())
                                "stats" -> navigator.push(StatisticsScreen())
                            }
                        }
                    )
                }
            }
        }

        // Features Section
        item {
            SectionHeader(
                title = "Features",
                onSeeAll = null
            )
        }

        items(getFeatures()) { feature ->
            FeatureListItem(
                feature = feature,
                onClick = {
                    when (feature.id) {
                        "notes" -> navigator.push(NotesListScreen())
                        "flashcards" -> navigator.push(FlashcardsScreen())
                        "study" -> navigator.push(StudySessionScreen())
                        "stats" -> navigator.push(StatisticsScreen())
                        "community" -> navigator.push(CommunityScreen())
                        "subjects" -> navigator.push(SubjectsScreen())
                    }
                }
            )
        }

        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun NotesTabContent(
    paddingValues: PaddingValues,
    navigator: Navigator
) {
    val noteRepository: NoteRepository = koinInject()
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        noteRepository.getAllNotes().collect { loadedNotes ->
            notes = loadedNotes
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Notes",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            IconButton(
                onClick = { navigator.push(NoteEditorScreen(noteId = null)) }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Note",
                    tint = PrimaryBlue
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search notes...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notes List
        val filteredNotes = notes.filter {
            searchQuery.isEmpty() || it.title.contains(searchQuery, ignoreCase = true) ||
                    it.content.contains(searchQuery, ignoreCase = true)
        }

        if (filteredNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Description,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (searchQuery.isEmpty()) "No notes yet" else "No notes found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                    if (searchQuery.isEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { navigator.push(NoteEditorScreen(noteId = null)) }) {
                            Text("Create your first note")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredNotes) { note ->
                    NoteCard(
                        note = note,
                        onClick = { navigator.push(NoteEditorScreen(noteId = note.id)) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                color = TextGray,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = note.subject,
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryBlue
                )
            }
        }
    }
}

@Composable
private fun FlashcardsTabContent(
    paddingValues: PaddingValues,
    navigator: Navigator
) {
    val flashcardRepository: FlashcardRepository = koinInject()
    var flashcards by remember { mutableStateOf<List<Flashcard>>(emptyList()) }

    LaunchedEffect(Unit) {
        flashcardRepository.getAllFlashcards().collect { loadedFlashcards ->
            flashcards = loadedFlashcards
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Flashcards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { navigator.push(FlashcardsScreen()) }) {
                Text("See All", color = PrimaryBlue)
            }
        }

        if (flashcards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Style,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No flashcards yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Create flashcards from your notes",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    FlashcardGroupCard(
                        subject = "All Flashcards",
                        cardCount = flashcards.size,
                        onClick = { navigator.push(FlashcardsScreen()) }
                    )
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun FlashcardGroupCard(
    subject: String,
    cardCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Style,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$cardCount cards",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = TextMuted
            )
        }
    }
}

@Composable
private fun CommunityTabContent(paddingValues: PaddingValues) {
    val navigator = LocalNavigator.currentOrThrow

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Text(
            text = "Community",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Groups,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Discover shared notes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { navigator.push(CommunityScreen()) },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Explore Community")
                }
            }
        }
    }
}

@Composable
private fun StatisticsTabContent(paddingValues: PaddingValues) {
    val noteRepository: NoteRepository = koinInject()
    val flashcardRepository: FlashcardRepository = koinInject()
    val studyRecordRepository: StudyRecordRepository = koinInject()

    var notesCount by remember { mutableStateOf(0) }
    var flashcardsCount by remember { mutableStateOf(0) }
    var dueCardsCount by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        noteRepository.getAllNotes().collect { notes ->
            notesCount = notes.size
        }
    }

    LaunchedEffect(Unit) {
        flashcardRepository.getAllFlashcards().collect { flashcards ->
            flashcardsCount = flashcards.size
        }
    }

    LaunchedEffect(Unit) {
        dueCardsCount = studyRecordRepository.countDueCards()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            StatCard(
                title = "Total Notes",
                value = "$notesCount",
                icon = Icons.Outlined.Description,
                color = PrimaryBlue
            )
        }

        item {
            StatCard(
                title = "Total Flashcards",
                value = "$flashcardsCount",
                icon = Icons.Outlined.Style,
                color = AccentPurple
            )
        }

        item {
            StatCard(
                title = "Cards Due for Review",
                value = "$dueCardsCount",
                icon = Icons.Outlined.Schedule,
                color = WarningOrange
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SubjectsTabContent(paddingValues: PaddingValues) {
    val subjectRepository: SubjectRepository = koinInject()
    var subjects by remember { mutableStateOf<List<Subject>>(emptyList()) }

    LaunchedEffect(Unit) {
        subjectRepository.ensureDefaultSubjectsExist()
        subjectRepository.getAllSubjects().collect { loadedSubjects ->
            subjects = loadedSubjects
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        // Header
        Text(
            text = "Subjects",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        )

        if (subjects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Category,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = TextMuted
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No subjects yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextMuted
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(subjects) { subject ->
                    SubjectCard(subject = subject)
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject) {
    val subjectColor = try {
        val colorString = subject.color.removePrefix("#")
        Color(colorString.toLong(16) or 0xFF000000)
    } catch (e: Exception) {
        PrimaryBlue
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(subjectColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${subject.noteCount} notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextGray
                )
            }
        }
    }
}

// ============== SHARED COMPONENTS ==============

@Composable
private fun StatsCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    progress: Float?,
    progressColor: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (progress != null) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = progressColor,
                    trackColor = DarkSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuoteCard(quote: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Purple accent bar on the left
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(IntrinsicSize.Max)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(AccentPurple, AccentPurpleLight)
                        ),
                        shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                    )
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyLarge,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 26.sp
                )
            }
        }
    }
}

@Composable
private fun StudyNowCard(
    dueCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = PrimaryBlue.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ready to Study!",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$dueCount cards due for review",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    onSeeAll: (() -> Unit)?
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (onSeeAll != null) {
            TextButton(onClick = onSeeAll) {
                Text(text = "See all >", color = PrimaryBlue)
            }
        }
    }
}

@Composable
private fun QuickActionCard(
    action: QuickAction,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(action.color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = null,
                    tint = action.color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = action.title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun FeatureListItem(
    feature: Feature,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Colored dot indicator
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(feature.dotColor)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = feature.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = feature.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StudyBuddyBottomNav(
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = DarkSurface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp
    ) {
        val items = listOf(
            BottomNavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
            BottomNavItem("Notes", Icons.Filled.Description, Icons.Outlined.Description),
            BottomNavItem("Cards", Icons.Filled.Style, Icons.Outlined.Style),
            BottomNavItem("Community", Icons.Filled.Groups, Icons.Outlined.Groups),
            BottomNavItem("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
            BottomNavItem("Subjects", Icons.Filled.Category, Icons.Outlined.Category)
        )

        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = if (selectedItem == index) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedItem == index) FontWeight.Medium else FontWeight.Normal
                    )
                },
                selected = selectedItem == index,
                onClick = { onItemSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryBlue,
                    selectedTextColor = PrimaryBlue,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ============== DATA CLASSES ==============

private data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private data class QuickAction(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color
)

private data class Feature(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val dotColor: Color
)

// ============== DATA PROVIDERS ==============

private fun getQuickActions() = listOf(
    QuickAction("notes", "Notes", Icons.Default.Description, PrimaryBlue),
    QuickAction("flashcards", "Cards", Icons.Default.Style, AccentPurple),
    QuickAction("study", "Study", Icons.Default.School, SuccessGreen),
    QuickAction("community", "Community", Icons.Default.Groups, WarningOrange),
    QuickAction("subjects", "Subjects", Icons.Default.Category, InfoBlue),
    QuickAction("stats", "Stats", Icons.Default.BarChart, DotPurple)
)

private fun getFeatures() = listOf(
    Feature(
        id = "notes",
        title = "My Notes",
        description = "Create and manage your study notes",
        icon = Icons.Default.Description,
        dotColor = DotRed
    ),
    Feature(
        id = "flashcards",
        title = "Flashcards",
        description = "View and organize your flashcards",
        icon = Icons.Default.Style,
        dotColor = DotOrange
    ),
    Feature(
        id = "study",
        title = "Study Session",
        description = "Practice with spaced repetition",
        icon = Icons.Default.School,
        dotColor = DotBlue
    ),
    Feature(
        id = "community",
        title = "Community",
        description = "Discover and share notes with students",
        icon = Icons.Default.Groups,
        dotColor = DotGreen
    ),
    Feature(
        id = "subjects",
        title = "Subjects",
        description = "Manage and organize your subjects",
        icon = Icons.Default.Category,
        dotColor = DotPurple
    ),
    Feature(
        id = "stats",
        title = "Statistics",
        description = "Track your learning progress",
        icon = Icons.Default.BarChart,
        dotColor = PrimaryBlue
    )
)
