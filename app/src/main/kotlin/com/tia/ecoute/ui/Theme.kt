package com.tia.ecoute.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

class Palette(
    val name: String,
    val navy: Color, val navyLight: Color,
    val violet: Color, val magenta: Color, val blue: Color,
    val cyan: Color, val orange: Color, val yellow: Color
)

object Palettes {
    val all = listOf(
        Palette("Néon TIA", Color(0xFF080419), Color(0xFF150B35), Color(0xFF8E2DF7), Color(0xFFE23CFF), Color(0xFF2F6BFF), Color(0xFF19C2FF), Color(0xFFFF9A1F), Color(0xFFFFD321)),
        Palette("Océan", Color(0xFF03111F), Color(0xFF082338), Color(0xFF1F7AE0), Color(0xFF00C2D1), Color(0xFF1450C8), Color(0xFF5CF2E1), Color(0xFF7DD3FC), Color(0xFFB6F3FF)),
        Palette("Coucher de soleil", Color(0xFF1A0A12), Color(0xFF2E1220), Color(0xFFD6336C), Color(0xFFFF4D6D), Color(0xFFFF7A3D), Color(0xFFFFB84D), Color(0xFFFF8F3D), Color(0xFFFFD23F)),
        Palette("Forêt", Color(0xFF04140C), Color(0xFF0A2418), Color(0xFF1F9D55), Color(0xFF3DDC84), Color(0xFF15803D), Color(0xFF9BF6B2), Color(0xFFC5E34B), Color(0xFFF2F36B)),
        Palette("Rose bonbon", Color(0xFF1C0618), Color(0xFF2F0B2A), Color(0xFFC026D3), Color(0xFFFF4FD8), Color(0xFF7C3AED), Color(0xFFF9A8D4), Color(0xFFFB7185), Color(0xFFFDE68A)),
        Palette("AMOLED noir", Color(0xFF000000), Color(0xFF0A0A0A), Color(0xFF8E2DF7), Color(0xFFE23CFF), Color(0xFF2F6BFF), Color(0xFF19C2FF), Color(0xFFFF9A1F), Color(0xFFFFD321))
    )
}

object ThemeState {
    var index by mutableIntStateOf(0)
    val p: Palette get() = Palettes.all[index.coerceIn(0, Palettes.all.lastIndex)]
}

val Navy: Color get() = ThemeState.p.navy
val NavyLight: Color get() = ThemeState.p.navyLight
val Violet: Color get() = ThemeState.p.violet
val Magenta: Color get() = ThemeState.p.magenta
val Blue: Color get() = ThemeState.p.blue
val Cyan: Color get() = ThemeState.p.cyan
val Orange: Color get() = ThemeState.p.orange
val Yellow: Color get() = ThemeState.p.yellow
val SheetBg: Color get() = ThemeState.p.navyLight
val TextHi = Color(0xFFF4F1FF)
val TextLo = Color(0xFFA9A0C9)

val BrandBrush: Brush get() = Brush.linearGradient(listOf(Magenta, Violet, Blue))
val WarmBrush: Brush get() = Brush.linearGradient(listOf(Yellow, Orange, Magenta))
val CoolBrush: Brush get() = Brush.linearGradient(listOf(Violet, Blue, Cyan))

@Composable
fun TiaTheme(content: @Composable () -> Unit) {
    val p = ThemeState.p
    val scheme = remember(ThemeState.index) {
        darkColorScheme(
            primary = p.violet, onPrimary = Color.White, secondary = p.cyan, tertiary = p.orange,
            background = p.navy, onBackground = TextHi, surface = p.navyLight, onSurface = TextHi,
            surfaceVariant = p.navyLight, onSurfaceVariant = TextLo,
            surfaceContainer = p.navyLight, surfaceContainerHigh = p.navyLight
        )
    }
    MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
}
