package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class CivicSemanticColors(
    val eligible: Color,
    val eligibleContainer: Color,
    val onEligibleContainer: Color,
    val partial: Color,
    val partialContainer: Color,
    val onPartialContainer: Color,
    val ineligible: Color,
    val ineligibleContainer: Color,
    val onIneligibleContainer: Color,
    val headerStart: Color,
    val headerEnd: Color,
    val onHeader: Color,
    val saffronHighlight: Color
)

private val LightCivicSemanticColors = CivicSemanticColors(
    eligible = EligibleGreenLight,
    eligibleContainer = EligibleGreenContainerLight,
    onEligibleContainer = OnEligibleGreenContainerLight,
    partial = PartialAmberLight,
    partialContainer = PartialAmberContainerLight,
    onPartialContainer = OnPartialAmberContainerLight,
    ineligible = IneligibleRedLight,
    ineligibleContainer = IneligibleRedContainerLight,
    onIneligibleContainer = OnIneligibleRedContainerLight,
    headerStart = AshokaIndigo,
    headerEnd = AshokaIndigoLight,
    onHeader = CrispPaper,
    saffronHighlight = SaffronAccent
)

private val DarkCivicSemanticColors = CivicSemanticColors(
    eligible = EligibleGreenDark,
    eligibleContainer = EligibleGreenContainerDark,
    onEligibleContainer = OnEligibleGreenContainerDark,
    partial = PartialAmberDark,
    partialContainer = PartialAmberContainerDark,
    onPartialContainer = OnPartialAmberContainerDark,
    ineligible = IneligibleRedDark,
    ineligibleContainer = IneligibleRedContainerDark,
    onIneligibleContainer = OnIneligibleRedContainerDark,
    headerStart = Color(0xFF0E1D3B),
    headerEnd = Color(0xFF19305C),
    onHeader = DarkInkPrimary,
    saffronHighlight = DarkSecondary
)

val LocalCivicColors = staticCompositionLocalOf { LightCivicSemanticColors }

private val LightColorScheme = lightColorScheme(
    primary = AshokaIndigo,
    onPrimary = CrispPaper,
    primaryContainer = AshokaIndigoContainer,
    onPrimaryContainer = OnAshokaIndigoContainer,
    secondary = SaffronAccent,
    onSecondary = CrispPaper,
    secondaryContainer = SaffronContainer,
    onSecondaryContainer = OnSaffronContainer,
    tertiary = CivicTeal,
    onTertiary = CrispPaper,
    tertiaryContainer = CivicTealContainer,
    onTertiaryContainer = OnCivicTealContainer,
    background = ParchmentCanvas,
    onBackground = InkPrimary,
    surface = CrispPaper,
    onSurface = InkPrimary,
    surfaceVariant = SoftStoneSurface,
    onSurfaceVariant = InkSecondary,
    surfaceContainerLowest = CrispPaper,
    surfaceContainerLow = Color(0xFFF7F4EE),
    surfaceContainer = SoftStoneSurface,
    surfaceContainerHigh = Color(0xFFEBE6DC),
    outline = SoftStoneBorder,
    outlineVariant = Color(0xFFE8E2D7),
    error = IneligibleRedLight,
    onError = CrispPaper,
    errorContainer = IneligibleRedContainerLight,
    onErrorContainer = OnIneligibleRedContainerLight
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),
    background = DarkCanvas,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkInkSecondary,
    surfaceContainerLowest = Color(0xFF0A101B),
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurfaceVariant,
    surfaceContainerHigh = Color(0xFF253553),
    outline = DarkOutline,
    outlineVariant = Color(0xFF24324D),
    error = IneligibleRedDark,
    onError = Color(0xFF450A0A),
    errorContainer = IneligibleRedContainerDark,
    onErrorContainer = OnIneligibleRedContainerDark
)

private val JanSetuShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JanSetuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val civicColors = if (darkTheme) DarkCivicSemanticColors else LightCivicSemanticColors

    CompositionLocalProvider(LocalCivicColors provides civicColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = JanSetuShapes,
            content = content
        )
    }
}
