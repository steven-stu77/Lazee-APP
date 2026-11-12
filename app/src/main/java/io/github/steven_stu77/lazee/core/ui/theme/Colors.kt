package io.github.steven_stu77.lazee.core.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.steven_stu77.lazee.core.prefs.ThemeKey

/** Colours that never change with the theme (spec §9). */
object Base {
    val page = Color(0xFFF5F6F8)
    val card = Color(0xFFFFFFFF)
    val text = Color(0xFF14161A)
    val textSub = Color(0xFF5F6672)
    val divider = Color(0xFFEEF0F3)
    val border = Color(0xFFDADDE3)
    val switchOff = Color(0xFFC9CDD4)
    val disabled = Color(0xFFA9B0BA)
    val warnBg = Color(0xFFFFF3DC)
    val warnText = Color(0xFF6E4400)
    val warnStrong = Color(0xFF8A5300)
    val infoBg = Color(0xFFE6F0FB)
    val infoText = Color(0xFF1D4E89)
    val dangerText = Color(0xFFB42318)
    val danger = Color(0xFFD92D20)
    val orange = Color(0xFFE39A1E)
    val yellowBar = Color(0xFFFFF3DC)
}

data class Palette(
    val key: ThemeKey,
    val accent: Color,
    val deep: Color,
    val tint: Color,
    val tintText: Color,
    val glow: Color,
    val sub: Color,
    val sub2: Color,
    val soft: Color,
    val chart: List<Color>,
    val heat: List<Color>,
)

private fun c(hex: Long) = Color(0xFF000000 or hex)

object Palettes {
    val green = Palette(
        ThemeKey.green, c(0x0E7C66), c(0x12352E), c(0xE3F4EE), c(0x0A5C4C), c(0x7EE0C3), c(0xB9D6CE), c(0xD5E8E2), c(0xB7DDD3),
        listOf(0x12352E, 0x1F6B5A, 0x3F9C85, 0x7CC5B2, 0xB7DDD3, 0xD8EAE5, 0xE9F2EF).map(::c),
        listOf(0xCDE8E0, 0x8CCBB8, 0x3F9C85, 0x12352E).map(::c),
    )
    val blue = Palette(
        ThemeKey.blue, c(0x2457C5), c(0x142A55), c(0xE6EEFB), c(0x1D428F), c(0x9DBDF5), c(0xB8C8E8), c(0xD3DEF2), c(0xB3C8EF),
        listOf(0x142A55, 0x1F4590, 0x3B6BCB, 0x7A9FE0, 0xB3C8EF, 0xD6E2F7, 0xE8EEF9).map(::c),
        listOf(0xD6E2F7, 0xA9C1EE, 0x6E95DE, 0x142A55).map(::c),
    )
    val terra = Palette(
        ThemeKey.terra, c(0xB4441A), c(0x4A1F0F), c(0xFBEDE6), c(0x8F3412), c(0xF5B596), c(0xE5C3B3), c(0xF0D9CE), c(0xEDB196),
        listOf(0x4A1F0F, 0x7E3417, 0xB4441A, 0xD9744A, 0xEDB196, 0xF6D6C7, 0xFBEDE6).map(::c),
        listOf(0xF8E0D4, 0xF0BCA3, 0xE08A63, 0x4A1F0F).map(::c),
    )
    val grape = Palette(
        ThemeKey.grape, c(0x6941C6), c(0x2A1B54), c(0xF0EBFB), c(0x53359E), c(0xC4B1F2), c(0xCEC3EA), c(0xE0D8F3), c(0xBFAEEC),
        listOf(0x2A1B54, 0x45308A, 0x6941C6, 0x9277DB, 0xBFAEEC, 0xDDD3F6, 0xF0EBFB).map(::c),
        listOf(0xE6DEF8, 0xC9B8F0, 0xA189E0, 0x2A1B54).map(::c),
    )
    val ink = Palette(
        ThemeKey.ink, c(0x14161A), c(0x14161A), c(0xEEF0F3), c(0x14161A), c(0xE6E8EC), c(0xA9B0BA), c(0xC9CDD4), c(0xC9CDD4),
        listOf(0x14161A, 0x3F4752, 0x5F6672, 0x8A9099, 0xB4B9C1, 0xD3D6DB, 0xE6E8EC).map(::c),
        listOf(0xE6E8EC, 0xC9CDD4, 0x9AA0A8, 0x14161A).map(::c),
    )

    fun of(key: ThemeKey): Palette = when (key) {
        ThemeKey.green -> green
        ThemeKey.blue -> blue
        ThemeKey.terra -> terra
        ThemeKey.grape -> grape
        ThemeKey.ink -> ink
    }

    val all = listOf(green, blue, terra, grape, ink)
}

/** 8 category colours (background, foreground); fixed across themes. */
object CategoryColors {
    val pairs = listOf(
        c(0xFDECE3) to c(0x9A3B12),
        c(0xE6F0FB) to c(0x1D4E89),
        c(0xF1ECFB) to c(0x5B3A9E),
        c(0xFCE8F0) to c(0x9B2459),
        c(0xE3F4EE) to c(0x0A5C4C),
        c(0xFFF6DD) to c(0x7A5A00),
        c(0xFBEAEA) to c(0x9B2C2C),
        c(0xEEF0F3) to c(0x3F4752),
    )

    fun bg(i: Int) = pairs.getOrElse(i) { pairs[7] }.first
    fun fg(i: Int) = pairs.getOrElse(i) { pairs[7] }.second
}
