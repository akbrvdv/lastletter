package com.kelompok6.lastletter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ─── Color Palette (Arena Dark Theme) ─────────────────────────────────────────
private val ArenaBlack       = Color(0xFF0A0A0F)
private val ArenaDeepPurple  = Color(0xFF1A1035)
private val NeonCyan         = Color(0xFF00E5FF)
private val NeonPurple       = Color(0xFF7C4DFF)
private val WarningAmber     = Color(0xFFFFAB00)
private val DangerRed        = Color(0xFFFF1744)
private val SurfaceCard      = Color(0xFF16102B)
private val OnSurface        = Color(0xFFE8E0FF)

private val LexilinkDarkColorScheme = darkColorScheme(
    primary          = NeonCyan,
    onPrimary        = ArenaBlack,
    primaryContainer = ArenaDeepPurple,
    secondary        = NeonPurple,
    onSecondary      = Color.White,
    tertiary         = WarningAmber,
    error            = DangerRed,
    background       = ArenaBlack,
    onBackground     = OnSurface,
    surface          = SurfaceCard,
    onSurface        = OnSurface,
)

/**
 * LastLetterTheme — dark arena theme untuk Lexilink.
 * Hanya dark mode; light mode tidak diperlukan untuk game kompetitif.
 *
 * Phase 3: Tambah custom Typography dengan Google Fonts (Rajdhani / Orbitron)
 * untuk feel game yang lebih kuat.
 */
@Composable
fun LastLetterTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LexilinkDarkColorScheme,
        content = content
    )
}
