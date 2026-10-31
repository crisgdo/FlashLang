package com.cristiano.flashlang.data.scheduler

import org.junit.Assert.assertEquals
import org.junit.Test

class SchedulerTest {
    private val now = 1_700_000_000_000L

    @Test
    fun firstGoodReviewSchedulesOneDay() {
        val result = Scheduler.review(CardScheduleState(), ReviewRating.GOOD, now)

        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(now + DAY, result.dueAt)
        assertEquals(2.5, result.easeFactor, 0.0)
    }

    @Test
    fun consecutiveGoodReviewsIncreaseIntervals() {
        val first = Scheduler.review(CardScheduleState(), ReviewRating.GOOD, now)
        val second = Scheduler.review(first, ReviewRating.GOOD, now)
        val third = Scheduler.review(second, ReviewRating.GOOD, now)

        assertEquals(1, first.intervalDays)
        assertEquals(3, second.intervalDays)
        assertEquals(8, third.intervalDays)
        assertEquals(3, third.repetitions)
    }

    @Test
    fun againResetsProgressAndSchedulesTenMinutes() {
        val result = Scheduler.review(CardScheduleState(2.5, 8, 3, 0), ReviewRating.AGAIN, now)

        assertEquals(0, result.intervalDays)
        assertEquals(0, result.repetitions)
        assertEquals(2.3, result.easeFactor, 0.0)
        assertEquals(now + TEN_MINUTES, result.dueAt)
    }

    @Test
    fun easeFactorNeverFallsBelowMinimum() {
        val hard = Scheduler.review(CardScheduleState(1.35, 3, 1, 0), ReviewRating.HARD, now)
        val again = Scheduler.review(CardScheduleState(1.35, 3, 1, 0), ReviewRating.AGAIN, now)

        assertEquals(1.3, hard.easeFactor, 0.0)
        assertEquals(1.3, again.easeFactor, 0.0)
    }

    @Test
    fun easyIncreasesIntervalAndEaseFactor() {
        val result = Scheduler.review(CardScheduleState(2.5, 0, 0, 0), ReviewRating.EASY, now)

        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(2.65, result.easeFactor, 0.0)
    }

    @Test
    fun hardUsesMinimumOneDayAndReducesEaseFactor() {
        val result = Scheduler.review(CardScheduleState(2.5, 0, 0, 0), ReviewRating.HARD, now)

        assertEquals(1, result.intervalDays)
        assertEquals(1, result.repetitions)
        assertEquals(2.35, result.easeFactor, 0.0)
    }

    private companion object {
        const val DAY = 86_400_000L
        const val TEN_MINUTES = 600_000L
    }
}
