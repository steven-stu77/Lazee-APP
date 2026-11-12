package io.github.steven_stu77.lazee.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.steven_stu77.lazee.R

val LocalPalette = staticCompositionLocalOf { Palettes.green }
val LocalHideAmounts = staticCompositionLocalOf { false }

/** Inter is a variable font; Chinese glyphs fall back to the system CJK font (Noto Sans CJK). */
@OptIn(ExperimentalTextApi::class)
private fun inter(weight: Int) = Font(
    R.font.inter,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val Inter = FontFamily(inter(400), inter(500), inter(600), inter(700))

object Type {
    val pageTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Base.text)
    val bigMonth = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 44.sp, letterSpacing = (-1.5).sp, color = Base.text)
    val heroAmount = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.5).sp)
    val addAmount = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 38.sp, letterSpacing = (-0.5).sp, color = Base.text)
    val section = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Base.text)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, color = Base.text)
    val bodyStrong = body.copy(fontWeight = FontWeight.SemiBold)
    val small = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, color = Base.textSub)
    val tiny = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, color = Base.textSub)
    val nav = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 11.sp)
}

object Dims {
    val pagePad = 20.dp
    val gap = 14.dp
    val heroRadius = RoundedCornerShape(20.dp)
    val cardRadius = RoundedCornerShape(16.dp)
    val buttonRadius = RoundedCornerShape(12.dp)
    val tagRadius = RoundedCornerShape(8.dp)
    val minTouch = 44.dp
}

@Composable
fun LazeeTheme(palette: Palette, hideAmounts: Boolean, content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = palette.accent,
        onPrimary = Color.White,
        primaryContainer = palette.tint,
        onPrimaryContainer = palette.tintText,
        secondary = palette.deep,
        background = Base.page,
        onBackground = Base.text,
        surface = Base.card,
        onSurface = Base.text,
        surfaceVariant = Base.divider,
        onSurfaceVariant = Base.textSub,
        outline = Base.border,
        error = Base.danger,
    )
    val typography = Typography().let { t ->
        Typography(
            bodyLarge = t.bodyLarge.copy(fontFamily = Inter),
            bodyMedium = t.bodyMedium.copy(fontFamily = Inter),
            bodySmall = t.bodySmall.copy(fontFamily = Inter),
            titleLarge = t.titleLarge.copy(fontFamily = Inter),
            titleMedium = t.titleMedium.copy(fontFamily = Inter),
            labelLarge = t.labelLarge.copy(fontFamily = Inter),
            labelMedium = t.labelMedium.copy(fontFamily = Inter),
        )
    }
    CompositionLocalProvider(LocalPalette provides palette, LocalHideAmounts provides hideAmounts) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = Shapes(medium = Dims.cardRadius, large = Dims.heroRadius),
            content = content,
        )
    }
}
