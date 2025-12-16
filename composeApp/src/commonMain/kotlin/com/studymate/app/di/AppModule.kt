package com.studymate.app.di

import com.studymate.app.ai.GeminiService
import com.studymate.app.ai.RateLimiter
import com.studymate.app.data.repository.FlashcardRepository
import com.studymate.app.data.repository.NoteRepository
import com.studymate.app.data.repository.StudyRecordRepository
import com.studymate.app.data.repository.SubjectRepository
import com.studymate.database.StudyMateDatabase
import org.koin.core.module.Module
import org.koin.dsl.module

val commonModule = module {
    // Database
    single { StudyMateDatabase(get()) }

    // Repositories
    single { NoteRepository(get()) }
    single { FlashcardRepository(get()) }
    single { StudyRecordRepository(get()) }
    single { SubjectRepository(get()) }

    // AI Services
    single { RateLimiter() }
    single { GeminiService(get()) }
}

expect fun platformModule(): Module
