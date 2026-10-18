package com.example.appfinanzas.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import kotlinx.coroutines.delay

data class SmartInsight(
    val id: Int,
    val category: String,
    val iconEmoji: String,
    val title: String,
    val message: String,
    val actionText: String? = null,
    val accentColor: Color,
)

@Composable
fun SmartInsightsBanner(
    modifier: Modifier = Modifier,
    onActionClick: ((SmartInsight) -> Unit)? = null,
) {
    val insights = remember {
        listOf(
            SmartInsight(
                id = 1,
                category = "DISPONIBLE IA",
                iconEmoji = "💡",
                title = "Optimización de Cupo",
                message = "Tu disponible real te permite abonar $50.000 a la cuota de tu tarjeta sin afectar tus gastos fijos de este mes.",
                actionText = "Abonar $50.000",
                accentColor = Emerald500
            ),
            SmartInsight(
                id = 2,
                category = "ALERTA HORMIGA",
                iconEmoji = "⚡",
                title = "Progreso Positivo",
                message = "¡Excelente! Tus compras impulsivas y gastos hormiga bajaron un 18% respecto al mismo periodo del mes anterior.",
                actionText = "Ver desglose",
                accentColor = Violet500
            ),
            SmartInsight(
                id = 3,
                category = "PROYECCIÓN PRO",
                iconEmoji = "🤖",
                title = "Superávit Proyectado",
                message = "Se prevé un saldo excedente de $120.000 al cierre de mes. ¿Deseas programar una transferencia automática a tu Meta de Viaje?",
                actionText = "Ahorrar $120k",
                accentColor = Emerald500
            ),
            SmartInsight(
                id = 4,
                category = "RECOMENDACIÓN SMART",
                iconEmoji = "🎯",
                title = "Fechas Clave de Pagos",
                message = "Tienes 2 servicios fijos por vencer en 4 días. El saldo disponible actual cubre el 100% sin riesgo.",
                actionText = "Revisar Fijos",
                accentColor = Violet500
            )
        )
    }

    var currentIndex by remember { mutableIntStateOf(0) }
    var isDismissed by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(currentIndex, isDismissed) {
        if (!isDismissed) {
            delay(8000)
            currentIndex = (currentIndex + 1) % insights.size
        }
    }

    if (isDismissed) return

    val currentInsight = insights[currentIndex]

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "borderAlpha"
    )

    AnimatedVisibility(
        visible = !isDismissed,
        exit = fadeOut() + shrinkVertically()
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            currentInsight.accentColor.copy(alpha = borderAlpha),
                            GlassBorder,
                            Violet500.copy(alpha = borderAlpha)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                ),
            shape = RoundedCornerShape(24.dp),
            color = Slate900.copy(alpha = 0.88f),
            shadowElevation = 8.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                currentInsight.accentColor.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = currentInsight.accentColor.copy(alpha = 0.18f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    currentInsight.accentColor.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = currentInsight.accentColor,
                                        modifier = Modifier
                                            .size(14.dp)
                                            .scale(glowScale)
                                    )
                                    Text(
                                        text = "PARDO IA 2026",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = currentInsight.accentColor,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            Text(
                                text = currentInsight.category,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // Dot indicators
                            insights.forEachIndexed { idx, _ ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (idx == currentIndex) 14.dp else 6.dp, 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (idx == currentIndex) currentInsight.accentColor else Slate400.copy(alpha = 0.3f)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Previous button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentIndex = if (currentIndex - 1 < 0) insights.size - 1 else currentIndex - 1
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .bounceClick(scaleDown = 0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronLeft,
                                    contentDescription = "Anterior insight",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Next button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    currentIndex = (currentIndex + 1) % insights.size
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .bounceClick(scaleDown = 0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = "Siguiente insight",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            // Dismiss / Close button
                            IconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    isDismissed = true
                                },
                                modifier = Modifier
                                    .size(26.dp)
                                    .bounceClick(scaleDown = 0.85f)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Cerrar banner",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    AnimatedContent(
                        targetState = currentInsight,
                        transitionSpec = {
                            (slideInVertically { height -> height } + fadeIn(animationSpec = tween(300)))
                                .togetherWith(slideOutVertically { height -> -height } + fadeOut(animationSpec = tween(200)))
                        },
                        label = "insightTransition"
                    ) { insight ->
                        Column {
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = insight.iconEmoji,
                                    fontSize = 22.sp
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = insight.title,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = insight.message,
                                        fontSize = 12.sp,
                                        color = Slate200,
                                        lineHeight = 17.sp,
                                        fontWeight = FontWeight.Normal
                                    )
                                }
                            }

                            if (insight.actionText != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .bounceClick {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onActionClick?.invoke(insight)
                                            },
                                        color = insight.accentColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            insight.accentColor.copy(alpha = 0.4f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = insight.actionText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = insight.accentColor
                                            )
                                            Icon(
                                                imageVector = Icons.Rounded.ChevronRight,
                                                contentDescription = null,
                                                tint = insight.accentColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
