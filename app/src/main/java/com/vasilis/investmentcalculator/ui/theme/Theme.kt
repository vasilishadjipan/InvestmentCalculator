package com.vasilis.investmentcalculator.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FinancePrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkGreenContainer,
    onPrimaryContainer = DarkText,

    secondary = FinanceTertiary,
    onSecondary = Color.White,
    secondaryContainer = DarkSurface,
    onSecondaryContainer = DarkText,

    tertiary = AccentGold,
    onTertiary = DarkBackground,
    tertiaryContainer = Color(0xFF654D2B),
    onTertiaryContainer = DarkText,

    background = DarkBackground,
    onBackground = DarkText,

    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkText,

    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = Color(0xFF192F49),
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = Color(0xFF294362),
    surfaceContainerHighest = Color(0xFF334D6B),

    surfaceBright = Color(0xFF3B5470),
    surfaceDim = DarkBackground,

    inverseSurface = LightBackground,
    inverseOnSurface = TextPrimary,
    inversePrimary = FinanceSecondary,

    outline = DarkOutline,
    outlineVariant = Color(0xFF3A536D),

    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = DarkErrorContainer,
    onErrorContainer = Color(0xFFFFDAD6)
)

private val LightColorScheme = lightColorScheme(
    primary = FinanceSecondary,
    onPrimary = Color.White,
    primaryContainer = LightGreenContainer,
    onPrimaryContainer = FinanceSecondary,

    secondary = FinanceTertiary,
    onSecondary = Color.White,
    secondaryContainer = LightGreenContainer,
    onSecondaryContainer = FinanceSecondary,

    tertiary = Color(0xFF806000),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE8B2),
    onTertiaryContainer = Color(0xFF382900),

    background = LightBackground,
    onBackground = TextPrimary,

    surface = LightSurface,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFFE7EEE8),
    onSurfaceVariant = TextSecondary,

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF8FAF6),
    surfaceContainer = Color(0xFFF0F4EE),
    surfaceContainerHigh = Color(0xFFE9EFE8),
    surfaceContainerHighest = Color(0xFFDDE6DD),

    surfaceBright = Color.White,
    surfaceDim = Color(0xFFD8E1D7),

    inverseSurface = DarkSurface,
    inverseOnSurface = DarkText,
    inversePrimary = FinancePrimary,

    outline = LightOutline,
    outlineVariant = Color(0xFFDCE5DE),

    error = Color(0xFFB3261E),
    onError = Color.White,
    errorContainer = LightErrorContainer,
    onErrorContainer = Color(0xFF410E0B)
)


@Composable
fun InvestmentCalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
