package com.studymate.app.ai

import com.studymate.app.PlatformConfig
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for interacting with Groq AI API (faster, more generous free tier)
 */
class GeminiService(
    private val rateLimiter: RateLimiter
) {
    private val apiKey = PlatformConfig.geminiApiKey
    private val baseUrl = "https://api.groq.com/openai/v1"
    private val json = Json { ignoreUnknownKeys = true }

    private val httpClient = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    /**
     * Summarize study notes into key concepts
     */
    suspend fun summarizeNotes(noteText: String): Result<String> {
        val prompt = """
            Analyze the following study notes and provide a concise summary.
            Extract the 5 most important concepts and key points.
            Format the response in clear, bullet-point style.
            Keep it brief and focused on what's essential to remember.

            Notes:
            $noteText
        """.trimIndent()

        return generateContent(prompt)
    }

    /**
     * Generate flashcards from study notes
     */
    suspend fun generateFlashcards(noteText: String, count: Int = 5): Result<List<FlashcardData>> {
        val prompt = """
            Create $count flashcards from these notes. Return ONLY a JSON array, no other text.
            Format: [{"question":"Q1","answer":"A1"},{"question":"Q2","answer":"A2"}]
            Keep answers short (under 50 words each).

            Notes: $noteText
        """.trimIndent()

        return try {
            val response = generateContent(prompt).getOrThrow()

            // Clean the response to extract JSON
            var jsonText = response.trim()

            // Remove markdown code blocks if present
            if (jsonText.contains("```")) {
                val startIndex = jsonText.indexOf('[')
                val endIndex = jsonText.lastIndexOf(']')
                if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                    jsonText = jsonText.substring(startIndex, endIndex + 1)
                }
            }

            // Find the JSON array in the response
            val arrayStart = jsonText.indexOf('[')
            val arrayEnd = jsonText.lastIndexOf(']')

            if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
                return Result.failure(Exception("No valid JSON array found in response"))
            }

            jsonText = jsonText.substring(arrayStart, arrayEnd + 1)

            val flashcards = json.decodeFromString<List<FlashcardData>>(jsonText)
            Result.success(flashcards)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to parse flashcards: ${e.message}", e))
        }
    }

    /**
     * Generate content using Groq API
     */
    private suspend fun generateContent(prompt: String): Result<String> {
        return rateLimiter.acquire {
            var lastException: Exception? = null
            var delayMs = 2000L

            repeat(3) { attempt ->
                try {
                    val response: HttpResponse = httpClient.post("$baseUrl/chat/completions") {
                        contentType(ContentType.Application.Json)
                        header("Authorization", "Bearer $apiKey")
                        setBody(
                            GroqRequest(
                                model = "llama-3.1-8b-instant",
                                messages = listOf(
                                    GroqMessage(role = "user", content = prompt)
                                ),
                                temperature = 0.7,
                                maxTokens = 2048
                            )
                        )
                    }

                    if (response.status.isSuccess()) {
                        val groqResponse = response.body<GroqResponse>()
                        val text = groqResponse.choices.firstOrNull()?.message?.content
                            ?: throw Exception("Empty response from Groq")
                        return@acquire Result.success(text)
                    } else if (response.status.value == 429) {
                        lastException = Exception("Rate limited. Retrying...")
                        delay(delayMs)
                        delayMs *= 2
                    } else {
                        val errorBody = response.bodyAsText()
                        return@acquire Result.failure(Exception("API request failed: ${response.status} - $errorBody"))
                    }
                } catch (e: Exception) {
                    lastException = e
                    if (attempt < 2) {
                        delay(delayMs)
                        delayMs *= 2
                    }
                }
            }

            Result.failure(Exception("AI API error: ${lastException?.message}", lastException))
        }
    }
}

/**
 * Data classes for flashcards
 */
@Serializable
data class FlashcardData(
    val question: String,
    val answer: String
)

/**
 * Data classes for Groq API
 */
@Serializable
private data class GroqRequest(
    val model: String,
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    @SerialName("max_tokens") val maxTokens: Int = 2048
)

@Serializable
private data class GroqMessage(
    val role: String,
    val content: String
)

@Serializable
private data class GroqResponse(
    val choices: List<GroqChoice>
)

@Serializable
private data class GroqChoice(
    val message: GroqMessage
)
