package com.studymate.app

import com.studymate.app.BuildConfig

/**
 * Android implementation of platform configuration
 * API key is loaded from local.properties via BuildConfig
 */
actual object PlatformConfig {
    actual val geminiApiKey: String = BuildConfig.GROQ_API_KEY
}
