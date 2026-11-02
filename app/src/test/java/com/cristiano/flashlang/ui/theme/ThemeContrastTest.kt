package com.cristiano.flashlang.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class ThemeContrastTest {

    private fun relativeLuminance(color: Color): Double {
        fun component(c: Float): Double {
            return if (c <= 0.04045f) {
                (c / 12.92).toDouble()
            } else {
                ((c + 0.055) / 1.055).toDouble().pow(2.4)
            }
        }
        val r = component(color.red)
        val g = component(color.green)
        val b = component(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(color1: Color, color2: Color): Double {
        val l1 = relativeLuminance(color1)
        val l2 = relativeLuminance(color2)
        val lighter = maxOf(l1, l2)
        val darker = minOf(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun assertMinContrast(
        foreground: Color,
        background: Color,
        minRatio: Double,
        pairDescription: String,
        mode: String,
    ) {
        val ratio = contrastRatio(foreground, background)
        assertTrue(
            "[$mode] $pairDescription ratio is $ratio, expected at least $minRatio",
            ratio >= minRatio,
        )
    }

    private fun validateColorSchemeContrasts(scheme: ColorScheme, mode: String) {
        // onSurface vs surface >= 4.5:1
        assertMinContrast(scheme.onSurface, scheme.surface, 4.5, "onSurface vs surface", mode)

        // onSurface vs surfaceContainers >= 4.5:1
        assertMinContrast(scheme.onSurface, scheme.surfaceContainerLowest, 4.5, "onSurface vs surfaceContainerLowest", mode)
        assertMinContrast(scheme.onSurface, scheme.surfaceContainerLow, 4.5, "onSurface vs surfaceContainerLow", mode)
        assertMinContrast(scheme.onSurface, scheme.surfaceContainer, 4.5, "onSurface vs surfaceContainer", mode)
        assertMinContrast(scheme.onSurface, scheme.surfaceContainerHigh, 4.5, "onSurface vs surfaceContainerHigh", mode)
        assertMinContrast(scheme.onSurface, scheme.surfaceContainerHighest, 4.5, "onSurface vs surfaceContainerHighest", mode)

        // onSurfaceVariant vs surfaceContainer >= 4.5:1
        assertMinContrast(scheme.onSurfaceVariant, scheme.surfaceContainer, 4.5, "onSurfaceVariant vs surfaceContainer", mode)

        // onPrimary vs primary >= 4.5:1
        assertMinContrast(scheme.onPrimary, scheme.primary, 4.5, "onPrimary vs primary", mode)

        // onPrimaryContainer vs primaryContainer >= 4.5:1
        assertMinContrast(scheme.onPrimaryContainer, scheme.primaryContainer, 4.5, "onPrimaryContainer vs primaryContainer", mode)

        // onSecondaryContainer vs secondaryContainer >= 4.5:1
        assertMinContrast(scheme.onSecondaryContainer, scheme.secondaryContainer, 4.5, "onSecondaryContainer vs secondaryContainer", mode)

        // onError vs error >= 4.5:1
        assertMinContrast(scheme.onError, scheme.error, 4.5, "onError vs error", mode)

        // outline vs surface >= 3.0:1
        assertMinContrast(scheme.outline, scheme.surface, 3.0, "outline vs surface", mode)
    }

    @Test
    fun darkColorScheme_meetsAllWcagContrastRequirements() {
        validateColorSchemeContrasts(DarkColorScheme, "Dark Mode (Option A: Indigo & Amber)")
        assertMinContrast(DarkExtendedColors.onSuccess, DarkExtendedColors.success, 4.5, "onSuccess vs success", "Dark Mode")
        assertMinContrast(DarkExtendedColors.onWarning, DarkExtendedColors.warning, 4.5, "onWarning vs warning", "Dark Mode")
        assertMinContrast(DarkExtendedColors.onSuccessContainer, DarkExtendedColors.successContainer, 4.5, "onSuccessContainer vs successContainer", "Dark Mode")
        assertMinContrast(DarkExtendedColors.onWarningContainer, DarkExtendedColors.warningContainer, 4.5, "onWarningContainer vs warningContainer", "Dark Mode")
    }

    @Test
    fun lightColorScheme_meetsAllWcagContrastRequirements() {
        validateColorSchemeContrasts(LightColorScheme, "Light Mode (Option A: Indigo & Amber)")
        assertMinContrast(LightExtendedColors.onSuccess, LightExtendedColors.success, 3.0, "onSuccess vs success", "Light Mode")
        assertMinContrast(LightExtendedColors.onWarning, LightExtendedColors.warning, 3.0, "onWarning vs warning", "Light Mode")
        assertMinContrast(LightExtendedColors.onSuccessContainer, LightExtendedColors.successContainer, 4.5, "onSuccessContainer vs successContainer", "Light Mode")
        assertMinContrast(LightExtendedColors.onWarningContainer, LightExtendedColors.warningContainer, 4.5, "onWarningContainer vs warningContainer", "Light Mode")
    }
}
