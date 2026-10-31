package com.cristiano.flashlang.data.scheduler

import kotlin.math.roundToInt

enum class ReviewRating { AGAIN, HARD, GOOD, EASY }

data class CardScheduleState(
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val dueAt: Long = 0,
)

object Scheduler {
    private const val MIN_EASE_FACTOR = 1.3
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000
    private const val TEN_MINUTES_MILLIS = 10L * 60 * 1000

    fun review(current: CardScheduleState, rating: ReviewRating, now: Long): CardScheduleState {
        val result = when (rating) {
            ReviewRating.AGAIN -> CardScheduleState(
                easeFactor = (current.easeFactor - 0.2).coerceAtLeast(MIN_EASE_FACTOR),
                intervalDays = 0,
                repetitions = 0,
                dueAt = now + TEN_MINUTES_MILLIS,
            )
            ReviewRating.HARD -> {
                val interval = (current.intervalDays * 1.2).roundToInt().coerceAtLeast(1)
                CardScheduleState(
                    easeFactor = (current.easeFactor - 0.15).coerceAtLeast(MIN_EASE_FACTOR),
                    intervalDays = interval,
                    repetitions = current.repetitions + 1,
                    dueAt = now + interval * DAY_MILLIS,
                )
            }
            ReviewRating.GOOD, ReviewRating.EASY -> {
                val baseInterval = when (current.repetitions) {
                    0 -> 1.0
                    1 -> 3.0
                    else -> current.intervalDays * current.easeFactor
                }
                val interval = (baseInterval * if (rating == ReviewRating.EASY) 1.3 else 1.0)
                    .roundToInt().coerceAtLeast(1)
                CardScheduleState(
                    easeFactor = if (rating == ReviewRating.EASY) current.easeFactor + 0.15 else current.easeFactor,
                    intervalDays = interval,
                    repetitions = current.repetitions + 1,
                    dueAt = now + interval * DAY_MILLIS,
                )
            }
        }
        return result
    }
}
