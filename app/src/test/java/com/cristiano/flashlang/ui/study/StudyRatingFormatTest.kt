package com.cristiano.flashlang.ui.study

import com.cristiano.flashlang.data.model.Card
import com.cristiano.flashlang.data.scheduler.ReviewRating
import org.junit.Assert.assertEquals
import org.junit.Test

class StudyRatingFormatTest {

    @Test
    fun formatIntervalDays_returnsExpectedRepresentations() {
        assertEquals("1d", formatIntervalDays(0))
        assertEquals("1d", formatIntervalDays(1))
        assertEquals("2d", formatIntervalDays(2))
        assertEquals("15d", formatIntervalDays(15))
        assertEquals("29d", formatIntervalDays(29))
        assertEquals("1m", formatIntervalDays(30))
        assertEquals("2m", formatIntervalDays(45))
        assertEquals("2m", formatIntervalDays(60))
        assertEquals("12m", formatIntervalDays(364))
        assertEquals("1a", formatIntervalDays(365))
        assertEquals("2a", formatIntervalDays(730))
    }

    @Test
    fun formatPredictedInterval_againRating_alwaysReturnsTenMinutes() {
        val newCard = Card(
            deckId = 1L,
            term = "test",
            type = "word",
            termTranslation = "teste",
            example = "test example",
            exampleTranslation = "exemplo teste",
        )
        val matureCard = newCard.copy(
            intervalDays = 60,
            repetitions = 5,
            easeFactor = 2.6,
        )

        assertEquals("10m", formatPredictedInterval(newCard, ReviewRating.AGAIN))
        assertEquals("10m", formatPredictedInterval(matureCard, ReviewRating.AGAIN))
    }

    @Test
    fun formatPredictedInterval_newCard_differentiatesInitialIntervals() {
        val card = Card(
            deckId = 1L,
            term = "hello",
            type = "word",
            termTranslation = "olá",
            example = "hello world",
            exampleTranslation = "olá mundo",
            repetitions = 0,
            intervalDays = 0,
            easeFactor = 2.5,
        )

        assertEquals("10m", formatPredictedInterval(card, ReviewRating.AGAIN))
        assertEquals("1d", formatPredictedInterval(card, ReviewRating.HARD))
        assertEquals("2d", formatPredictedInterval(card, ReviewRating.GOOD))
        assertEquals("4d", formatPredictedInterval(card, ReviewRating.EASY))
    }

    @Test
    fun formatPredictedInterval_cardAfterOneRepetition_predictsExpectedProgression() {
        val card = Card(
            deckId = 1L,
            term = "learn",
            type = "word",
            termTranslation = "aprender",
            example = "learn kotlin",
            exampleTranslation = "aprenda kotlin",
            repetitions = 1,
            intervalDays = 1,
            easeFactor = 2.5,
        )

        assertEquals("10m", formatPredictedInterval(card, ReviewRating.AGAIN))
        assertEquals("1d", formatPredictedInterval(card, ReviewRating.HARD))
        assertEquals("3d", formatPredictedInterval(card, ReviewRating.GOOD))
        assertEquals("4d", formatPredictedInterval(card, ReviewRating.EASY))
    }

    @Test
    fun formatPredictedInterval_matureCard_predictsDaysAndMonths() {
        val card = Card(
            deckId = 1L,
            term = "fluent",
            type = "word",
            termTranslation = "fluente",
            example = "fluent speaker",
            exampleTranslation = "falante fluente",
            repetitions = 3,
            intervalDays = 15,
            easeFactor = 2.5,
        )

        assertEquals("10m", formatPredictedInterval(card, ReviewRating.AGAIN))
        assertEquals("18d", formatPredictedInterval(card, ReviewRating.HARD))
        assertEquals("1m", formatPredictedInterval(card, ReviewRating.GOOD))
        assertEquals("2m", formatPredictedInterval(card, ReviewRating.EASY))
    }
}
