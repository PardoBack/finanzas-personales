package com.example.finanzaspersonales.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// =========================================================
// TOKENS ESTÁTICOS BASE (APPETITE UI & DRIBBLE FINANCE)
// =========================================================

// Modo Oscuro Oficial (Predeterminado)
val DarkDeepObsidian = Color(0xFF090D14)          // Canvas principal
val DarkCardBackground = Color(0xFF121722)        // Tarjetas Tier 1
val DarkCardElevated = Color(0xFF19202E)          // Tarjetas elevadas Tier 2
val DarkCardSurfaceLight = Color(0xFF212A3D)      // Chips y superficies interactivas
val DarkSubtleBorder = Color(0xFF212A3D)          // Borde sutil
val DarkGlassBorder = Color(0x1FFFFFFF)           // Borde vítreo
val DarkTextPrimary = Color(0xFFF8FAFC)           // Texto principal
val DarkTextSecondary = Color(0xFF94A3B8)         // Texto secundario
val DarkTextMuted = Color(0xFF64748B)             // Texto terciario

// Modo Claro Oficial
val LightCanvasBackground = Color(0xFFF4F6FA)     // Canvas claro
val LightCardBackground = Color(0xFFFFFFFF)       // Tarjetas blancas puras
val LightCardElevated = Color(0xFFF8FAFC)         // Superficie elevada suave
val LightCardSurfaceLight = Color(0xFFEEF2F7)     // Chips y contenedores
val LightSubtleBorder = Color(0xFFE2E8F0)         // Borde gris pizarra
val LightGlassBorder = Color(0x0D000000)          // Borde sutil
val LightTextPrimary = Color(0xFF0F172A)          // Texto casi negro
val LightTextSecondary = Color(0xFF64748B)        // Texto gris medio
val LightTextMuted = Color(0xFF94A3B8)            // Texto gris tenue

// Acentos cromáticos de alta identidad fintech
val CyanNeon = Color(0xFF38BDF8)                  // Primario Cyan / Sky
val CyanGlow = Color(0xFF0284C7)                  // Glow / Gradiente
val EmeraldGreen = Color(0xFF10B981)              // Ingresos / Positivo
val EmeraldBright = Color(0xFF34D399)             // Éxito / Al día
val RoseRed = Color(0xFFF43F5E)                   // Gastos / Reversión
val RoseSoft = Color(0xFFFB7185)                  // Gastos texto
val AmberGold = Color(0xFFF59E0B)                 // Alertas / Pendientes
val VioletAccent = Color(0xFF8B5CF6)              // CFO IA / Especial
val IndigoDeep = Color(0xFF1E1B4B)                // Nequi / Secundario

// =========================================================
// SISTEMA DE DISEÑO DINÁMICO (COMPOSITION LOCAL)
// =========================================================

data class AppColors(
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val cardElevated: Color,
    val cardSurfaceLight: Color,
    val subtleBorder: Color,
    val glassBorder: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val cyan: Color,
    val cyanGlow: Color,
    val emerald: Color,
    val emeraldBright: Color,
    val rose: Color,
    val roseSoft: Color,
    val amber: Color,
    val violet: Color
)

val DarkAppColors = AppColors(
    isDark = true,
    background = DarkDeepObsidian,
    cardBackground = DarkCardBackground,
    cardElevated = DarkCardElevated,
    cardSurfaceLight = DarkCardSurfaceLight,
    subtleBorder = DarkSubtleBorder,
    glassBorder = DarkGlassBorder,
    textPrimary = DarkTextPrimary,
    textSecondary = DarkTextSecondary,
    textMuted = DarkTextMuted,
    cyan = CyanNeon,
    cyanGlow = CyanGlow,
    emerald = EmeraldGreen,
    emeraldBright = EmeraldBright,
    rose = RoseRed,
    roseSoft = RoseSoft,
    amber = AmberGold,
    violet = VioletAccent
)

val LightAppColors = AppColors(
    isDark = false,
    background = LightCanvasBackground,
    cardBackground = LightCardBackground,
    cardElevated = LightCardElevated,
    cardSurfaceLight = LightCardSurfaceLight,
    subtleBorder = LightSubtleBorder,
    glassBorder = LightGlassBorder,
    textPrimary = LightTextPrimary,
    textSecondary = LightTextSecondary,
    textMuted = LightTextMuted,
    cyan = Color(0xFF0284C7),
    cyanGlow = Color(0xFF0369A1),
    emerald = Color(0xFF059669),
    emeraldBright = Color(0xFF10B981),
    rose = Color(0xFFE11D48),
    roseSoft = Color(0xFFF43F5E),
    amber = Color(0xFFD97706),
    violet = Color(0xFF7C3AED)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// Acceso directo idiomático para Compose
object AppTheme {
    val colors: AppColors
        @Composable
        @ReadOnlyComposable
        get() = LocalAppColors.current
}

// Retrocompatibilidad con código existente
val DeepObsidian: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.background

val CardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.cardBackground

val CardElevated: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.cardElevated

val CardSurfaceLight: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.cardSurfaceLight

val SubtleBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.subtleBorder

val GlassBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.glassBorder

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = AppTheme.colors.textMuted
