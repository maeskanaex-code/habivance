package app.habivance.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Amber accent — same in both modes
private val Amber = Color(0xFFF59E0B)
private val AmberDark = Color(0xFFFBBF24)

val LinearLightColors = lightColorScheme(
    primary = Amber,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFFFEF3C7),
    onPrimaryContainer = Color(0xFF78350F),

    secondary = Color(0xFF71717A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF4F4F5),
    onSecondaryContainer = Color(0xFF18181B),

    background = Color(0xFFFCFCFD),
    onBackground = Color(0xFF0A0A0A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),

    outline = Color(0xFFECECEE),
    outlineVariant = Color(0xFFECECEE),

    error = Color(0xFFDC2626),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val LinearDarkColors = darkColorScheme(
    primary = AmberDark,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = Color(0xFFFEF3C7),

    secondary = Color(0xFFA1A1AA),
    onSecondary = Color(0xFF18181B),
    secondaryContainer = Color(0xFF1A1C20),
    onSecondaryContainer = Color(0xFFE4E4E7),

    background = Color(0xFF0B0C0E),
    onBackground = Color(0xFFF7F7F8),

    surface = Color(0xFF131417),
    onSurface = Color(0xFFF7F7F8),
    surfaceVariant = Color(0xFF18191D),
    onSurfaceVariant = Color(0xFFA1A1AA),

    outline = Color(0xFF1E2024),
    outlineVariant = Color(0xFF1E2024),

    error = Color(0xFFFCA5A5),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2)
)
