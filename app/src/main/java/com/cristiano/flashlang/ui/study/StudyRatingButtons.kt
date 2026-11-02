package com.cristiano.flashlang.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cristiano.flashlang.data.model.Card as FlashCard
import com.cristiano.flashlang.data.scheduler.CardScheduleState
import com.cristiano.flashlang.data.scheduler.ReviewRating
import com.cristiano.flashlang.data.scheduler.Scheduler
import com.cristiano.flashlang.ui.theme.extendedColors
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

@Composable
fun StudyRatingBar(
    card: FlashCard,
    enabled: Boolean,
    onRate: (ReviewRating) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                subtitle = "Em breve • ${formatPredictedInterval(card, ReviewRating.AGAIN)}",
                icon = Icons.Outlined.Close,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                enabled = enabled,
                onClick = { onRate(ReviewRating.AGAIN) },
                modifier = Modifier.weight(1f),
            )
            StudyRatingCardButton(
                title = "Difícil",
                subtitle = "Rever depois • ${formatPredictedInterval(card, ReviewRating.HARD)}",
                icon = Icons.Outlined.BarChart,
                containerColor = MaterialTheme.extendedColors.warningContainer,
                contentColor = MaterialTheme.extendedColors.onWarningContainer,
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
                subtitle = "Com esforço • ${formatPredictedInterval(card, ReviewRating.GOOD)}",
                icon = Icons.Outlined.Check,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                enabled = enabled,
                onClick = { onRate(ReviewRating.GOOD) },
                modifier = Modifier.weight(1f),
            )
            StudyRatingCardButton(
                title = "Fácil",
                subtitle = "Sem esforço • ${formatPredictedInterval(card, ReviewRating.EASY)}",
                icon = Icons.Outlined.Star,
                containerColor = MaterialTheme.extendedColors.successContainer,
                contentColor = MaterialTheme.extendedColors.onSuccessContainer,
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
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .defaultMinSize(minHeight = 56.dp)
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
            Column(
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.85f),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
