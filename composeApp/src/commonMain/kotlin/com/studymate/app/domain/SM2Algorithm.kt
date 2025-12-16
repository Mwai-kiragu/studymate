package com.studymate.app.domain

import kotlinx.datetime.*

/**
 * Difficulty rating for flashcard review
 */
enum class Difficulty {
    AGAIN,   // Complete blackout, incorrect response
    HARD,    // Incorrect response, but remembered upon reflection  
    GOOD,    // Correct response with some effort
    EASY     // Perfect recall, no hesitation
}

/**
 * Represents the scheduling data for a flashcard
 */
data class CardSchedule(
    val nextReviewDate: Instant,
    val interval: Int,        // Days until next review
    val repetitions: Int,     // Number of successful reviews
    val easeFactor: Double    // Quality factor (1.3-2.5)
)

/**
 * Implementation of the SM-2 spaced repetition algorithm
 * 
 * This algorithm optimizes retention by scheduling reviews based on
 * performance, ensuring efficient long-term memorization.
 */
object SM2Algorithm {
    
    private const val MIN_EASE_FACTOR = 1.3
    private const val INITIAL_EASE_FACTOR = 2.5
    
    /**
     * Calculate when a flashcard should be reviewed next
     */
    fun calculateNextReview(
        difficulty: Difficulty,
        currentSchedule: CardSchedule = CardSchedule(
            nextReviewDate = Clock.System.now(),
            interval = 0,
            repetitions = 0,
            easeFactor = INITIAL_EASE_FACTOR
        )
    ): CardSchedule {
        val quality = when (difficulty) {
            Difficulty.AGAIN -> 0
            Difficulty.HARD -> 1
            Difficulty.GOOD -> 2
            Difficulty.EASY -> 3
        }
        
        // If answered incorrectly (quality < 2), reset the card
        if (quality < 2) {
            return CardSchedule(
                nextReviewDate = Clock.System.now().plus(1, DateTimeUnit.DAY, TimeZone.UTC),
                interval = 1,
                repetitions = 0,
                easeFactor = maxOf(MIN_EASE_FACTOR, currentSchedule.easeFactor - 0.2)
            )
        }
        
        // Calculate new ease factor
        // EF' = EF + (0.1 - (5 - q) * (0.08 + (5 - q) * 0.02))
        // Simplified for 4-button system: EF' = EF + (0.1 - (3 - q) * (0.08 + (3 - q) * 0.02))
        val newEaseFactor = maxOf(
            MIN_EASE_FACTOR,
            currentSchedule.easeFactor + (0.1 - (3 - quality) * (0.08 + (3 - quality) * 0.02))
        )
        
        // Calculate new interval
        val newInterval = when (currentSchedule.repetitions) {
            0 -> 1      // First review: 1 day
            1 -> 6      // Second review: 6 days
            else -> (currentSchedule.interval * newEaseFactor).toInt()
        }
        
        return CardSchedule(
            nextReviewDate = Clock.System.now().plus(newInterval, DateTimeUnit.DAY, TimeZone.UTC),
            interval = newInterval,
            repetitions = currentSchedule.repetitions + 1,
            easeFactor = newEaseFactor
        )
    }
    
    /**
     * Check if a card is due for review
     */
    fun isDue(schedule: CardSchedule, currentTime: Instant = Clock.System.now()): Boolean {
        return currentTime >= schedule.nextReviewDate
    }
}
