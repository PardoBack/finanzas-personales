package com.example.appfinanzas.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500

/**
 * SpatialGlowCard 2026:
 * Wraps content with an ambient spherical mesh gradient glow (Emerald Green & Electric Violet)
 * with continuous fluid motion and depth layer effects behind the surface.
 */
@Composable
fun SpatialGlowCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    backgroundColor: Color = Slate900.copy(alpha = 0.88f),
    emeraldGlowAlpha: Float = 0.35f,
    violetGlowAlpha: Float = 0.35f,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spatialMeshGlow")

    // Pulsing and shifting animations for Emerald Green sphere
    val emeraldScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emeraldScale"
    )

    val emeraldOffsetX by infiniteTransition.animateFloat(
        initialValue = -0.15f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emeraldOffsetX"
    )

    val emeraldOffsetY by infiniteTransition.animateFloat(
        initialValue = -0.10f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emeraldOffsetY"
    )

    // Shifting animations for Electric Violet sphere
    val violetScale by infiniteTransition.animateFloat(
        initialValue = 1.2f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violetScale"
    )

    val violetOffsetX by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = -0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violetOffsetX"
    )

    val violetOffsetY by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = -0.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(6500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "violetOffsetY"
    )

    // Border glowing animation
    val borderGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderGlowAlpha"
    )

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 1.2.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Emerald500.copy(alpha = borderGlowAlpha),
                        GlassBorder,
                        Violet500.copy(alpha = borderGlowAlpha)
                    )
                ),
                shape = shape
            )
            .background(backgroundColor)
    ) {
        // Ambient Spherical Mesh Gradient 2026 Background Layer
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .clip(shape)
        ) {
            val width = size.width
            val height = size.height

            // 1. Emerald Green Spherical Glow Blob (Top-Left quadrant motion)
            val emeraldCenter = Offset(
                x = width * (0.25f + emeraldOffsetX),
                y = height * (0.30f + emeraldOffsetY)
            )
            val emeraldRadius = (width.coerceAtLeast(height) * 0.65f) * emeraldScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Emerald500.copy(alpha = emeraldGlowAlpha),
                        Emerald500.copy(alpha = emeraldGlowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = emeraldCenter,
                    radius = emeraldRadius
                ),
                center = emeraldCenter,
                radius = emeraldRadius
            )

            // 2. Electric Violet Spherical Glow Blob (Bottom-Right quadrant motion)
            val violetCenter = Offset(
                x = width * (0.75f + violetOffsetX),
                y = height * (0.70f + violetOffsetY)
            )
            val violetRadius = (width.coerceAtLeast(height) * 0.70f) * violetScale

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Violet500.copy(alpha = violetGlowAlpha),
                        Violet500.copy(alpha = violetGlowAlpha * 0.35f),
                        Color.Transparent
                    ),
                    center = violetCenter,
                    radius = violetRadius
                ),
                center = violetCenter,
                radius = violetRadius
            )

            // 3. Subtle ambient light reflection beam across top edge
            drawLine(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Emerald500.copy(alpha = borderGlowAlpha * 0.5f),
                        Violet500.copy(alpha = borderGlowAlpha * 0.5f),
                        Color.Transparent
                    )
                ),
                start = Offset(0f, 1f),
                end = Offset(width, 1f),
                strokeWidth = 2f
            )
        }

        // Foreground Content
        content()
    }
}
