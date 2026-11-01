package uz.tanish.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Yuqori kontrastli, xotirjam ranglar. Qizil rang ishlatilmaydi (TZ, 8-bo'lim, 7-qoida).
val Background = Color(0xFFFDF8F0)
val OnBackground = Color(0xFF111111)
val Primary = Color(0xFF1F4E79)
val OnPrimary = Color(0xFFFFFFFF)
val Muted = Color(0xFF8A8A8A)

private val AppColors = lightColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    background = Background,
    onBackground = OnBackground,
    surface = Background,
    onSurface = OnBackground,
    // Material'ning standart qizil "error" rangini ham xotirjam rangga almashtiramiz.
    error = Primary,
    onError = OnPrimary,
)

private val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp),
    headlineLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp),
    bodyLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
)

@Composable
fun TanishTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AppColors, typography = AppTypography, content = content)
}
