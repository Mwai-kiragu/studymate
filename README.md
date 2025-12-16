# StudyMate - AI-Powered Study Companion

Your intelligent study partner, everywhere you learn

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.22-blue.svg)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-1.6.0-blue)](https://www.jetbrains.com/compose-multiplatform/)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

**StudyMate** is an Android study companion built with Kotlin Multiplatform that uses AI to transform your notes into interactive learning experiences. With spaced repetition algorithms, StudyMate helps you study smarter, not harder.

## Features

- **Smart Note Taking** - Rich text editor with organization by subject
- **AI Summarization** - Generate concise summaries using Groq AI
- **Flashcard Generation** - Automatically create study flashcards from notes
- **Spaced Repetition** - SM-2 algorithm for optimal long-term retention
- **Study Analytics** - Track progress and study streaks
- **Offline-First** - All data stored locally, works without internet

## Tech Stack

### Shared Code

- **Kotlin Multiplatform** - Cross-platform development
- **Compose Multiplatform** - Declarative UI framework
- **SQLDelight** - Type-safe SQL database
- **Ktor Client** - Networking & HTTP requests
- **Groq AI (Llama 3.1)** - AI features (summarization & flashcards)
- **Kotlinx Libraries** - Coroutines, Serialization, Datetime
- **Koin** - Dependency injection

### Platform-Specific

- **Android**: Material 3, Activity Compose

## Quick Start

### Prerequisites

- Android Studio (latest version)
- JDK 17+
- Groq API key from [Groq Console](https://console.groq.com/keys) (free)

### Setup

1. **Clone the repository:**

   ```bash
   git clone https://github.com/Mwai-kiragu/studymate.git
   cd studymate
   ```

2. **Configure API key:**

   ```bash
   cp local.properties.example local.properties
   # Edit local.properties and add your Groq API key:
   # GROQ_API_KEY=your_api_key_here
   ```

3. **Build and run:**

   ```bash
   ./gradlew :androidApp:installDebug
   ```

## Project Structure

```
StudyMate/
├── composeApp/           # Shared KMP module
│   ├── commonMain/       # Shared code
│   └── androidMain/      # Android-specific
└── androidApp/           # Android app module
```

## Development Status

**Current Phase:** Core Features ✅

- Project structure & configuration
- SQLDelight database schemas
- SM-2 spaced repetition algorithm
- Groq AI service integration
- Note-taking features
- Flashcard system
- Statistics dashboard

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Acknowledgments

- **JetBrains** - For Kotlin and Compose Multiplatform
- **Groq** - For fast AI inference API
- **Cash App** - For SQLDelight

---

**Built with Kotlin Multiplatform**
