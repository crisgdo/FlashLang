package com.cristiano.flashlang.ui.study

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cristiano.flashlang.data.model.Card as FlashCard
import com.cristiano.flashlang.data.scheduler.CardScheduleState
import com.cristiano.flashlang.data.scheduler.ReviewRating
import com.cristiano.flashlang.data.scheduler.Scheduler
import com.cristiano.flashlang.ui.theme.BadgeAmberBackgroundDark
import com.cristiano.flashlang.ui.theme.BadgeAmberBackgroundLight
import com.cristiano.flashlang.ui.theme.BadgeAmberTextDark
import com.cristiano.flashlang.ui.theme.BadgeAmberTextLight
import com.cristiano.flashlang.ui.theme.BadgeGreenBackgroundDark
import com.cristiano.flashlang.ui.theme.BadgeGreenBackgroundLight
import com.cristiano.flashlang.ui.theme.BadgeGreenTextDark
import com.cristiano.flashlang.ui.theme.BadgeGreenTextLight
import kotlin.math.roundToInt

fun formatPredictedInterval(
    card: FlashCard,
    rating: ReviewRating,
    now: Long = System.currentTimeMillis(),
): String {
    if (rating == ReviewRating.AGAIN) {
        return "10m"
    }

    if (card.repetitions == 0) {
        return when (rating) {
            ReviewRating.AGAIN -> "10m"
            ReviewRating.HARD -> "1d"
            ReviewRating.GOOD -> "2d"
            ReviewRating.EASY -> "4d"
        }
    }

    val current = CardScheduleState(
        easeFactor = card.easeFactor,
        intervalDays = card.intervalDays,
        repetitions = card.repetitions,
        dueAt = card.dueAt,
    )
    val next = Scheduler.review(current, rating, now)
    return formatIntervalDays(next.intervalDays)
}

fun formatIntervalDays(days: Int): String {
    return when {
        days <= 1 -> "1d"
        days < 30 -> "${days}d"
        days < 365 -> "${(days / 30.0).roundToInt().coerceAtLeast(1)}m"
        else -> "${(days / 365.0).roundToInt().coerceAtLeast(1)}a"
    }
}

fun formatRatingSubtitle(card: FlashCard, rating: ReviewRating): String {
    val interval = formatPredictedInterval(card, rating)
    return when {
        rating == ReviewRating.AGAIN -> "10 min"
        interval.endsWith("d") -> "${interval.dropLast(1)} d"
        interval.endsWith("m") -> "${interval.dropLast(1)} m"
        interval.endsWith("a") -> "${interval.dropLast(1)} a"
        else -> interval
    }
}

@Composable
fun StudyRatingBar(
    card: FlashCard,
    enabled: Boolean,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDark = isSystemInDarkTheme()
    val amberBg = if (isDark) BadgeAmberBackgroundDark else BadgeAmberBackgroundLight
    val amberText = if (isDark) BadgeAmberTextDark else BadgeAmberTextLight
    val greenBg = if (isDark) BadgeGreenBackgroundDark else BadgeGreenBackgroundLight
    val greenText = if (isDark) BadgeGreenTextDark else BadgeGreenTextLight

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StudyRatingCardButton(
                title = "Errei",
                subtitle = formatRatingSubtitle(card, ReviewRating.AGAIN),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                enabled = enabled,
                onClick = { onRate(ReviewRating.AGAIN) },
                modifier = Modifier.weight(1f),
            )
            StudyRatingCardButton(
                title = "Difícil",
                subtitle = formatRatingSubtitle(card, ReviewRating.HARD),
                containerColor = amberBg,
                contentColor = amberText,
                enabled = enabled,
                onClick = { onRate(ReviewRating.HARD) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StudyRatingCardButton(
                title = "Bom",
                subtitle = formatRatingSubtitle(card, ReviewRating.GOOD),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                enabled = enabled,
                onClick = { onRate(ReviewRating.GOOD) },
                modifier = Modifier.weight(1f),
            )
            StudyRatingCardButton(
                title = "Fácil",
                subtitle = formatRatingSubtitle(card, ReviewRating.EASY),
                containerColor = greenBg,
                contentColor = greenText,
                enabled = enabled,
                onClick = { onRate(ReviewRating.EASY) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StudyRatingCardButton(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                ),
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
            )
        }
    }
}
