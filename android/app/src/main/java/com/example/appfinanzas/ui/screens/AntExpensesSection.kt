package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.PestControl
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.AmberAlert
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.util.CurrencyUtils
import kotlinx.coroutines.delay

data class QuickAntChip(
    val emoji: String,
    val name: String,
    val amount: Double
)

@Composable
fun AntExpensesSection(
    currentDailyTotal: Double = 22500.0,
    dailyLimit: Double = 30000.0,
    onAddAntExpense: (String, Double) -> Unit = { _, _ -> }
) {
    val chips = remember {
        listOf(
            QuickAntChip("☕", "Café", 5000.0),
            QuickAntChip("🍫", "Snack", 8000.0),
            QuickAntChip("🚕", "Taxi", 15000.0),
            QuickAntChip("🥤", "Gaseosa", 4000.0),
            QuickAntChip("🍰", "Postre", 10000.0)
        )
    }

    val haptic = LocalHapticFeedback.current
    val progress = (currentDailyTotal / dailyLimit).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "antProgress"
    )

    // Dynamic bar color: Green -> Amber -> Red
    val barColor by animateColorAsState(
        targetValue = when {
            animatedProgress < 0.5f -> Emerald500
            animatedProgress < 0.85f -> AmberAlert
            else -> RedAlert
        },
        animationSpec = tween(durationMillis = 500),
        label = "barColor"
    )

    var addedToastMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(addedToastMessage) {
        if (addedToastMessage != null) {
            delay(2200)
            addedToastMessage = null
        }
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Violet500.copy(alpha = 0.08f),
        borderColor = Violet500.copy(alpha = 0.35f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Violet500.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PestControl,
                        contentDescription = null,
                        tint = Violet500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "Gastos Hormiga",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Control de fugas diarias",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }
            }

            Surface(
                color = Violet500.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Violet500.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "1-Tap Quick",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Violet500
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1-Tap Quick Chips
        Text(
            text = "Registrar con 1 Tap (Expansión Fluida):",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = Slate200
        )

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(chips) { chip ->
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .bounceClick(scaleDown = 0.88f) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAddAntExpense(chip.name, chip.amount)
                            addedToastMessage = "⚡ + ${chip.emoji} ${chip.name} registrado (${CurrencyUtils.formatCOP(chip.amount)})"
                        },
                    color = Slate800,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Brush.linearGradient(
                            listOf(Violet500.copy(alpha = 0.5f), Slate700)
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(chip.emoji, fontSize = 14.sp)
                        Column {
                            Text(
                                chip.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Text(
                                CurrencyUtils.formatCOP(chip.amount),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Violet500
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Agregar",
                            tint = Violet500,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        // Continuous Liquid Expansion Toast Banner
        AnimatedVisibility(
            visible = addedToastMessage != null,
            enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring()) + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Emerald500.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = addedToastMessage ?: "",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald500
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Daily Threshold Progress Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (animatedProgress >= 0.85f) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = RedAlert,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Umbral Diario: ${(animatedProgress * 100).toInt()}%",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = barColor
                )
            }
            Text(
                text = "${CurrencyUtils.formatCOP(currentDailyTotal)} / ${CurrencyUtils.formatCOP(dailyLimit)}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate400
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Animated Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Slate700)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                barColor.copy(alpha = 0.7f),
                                barColor
                            )
                        )
                    )
            )
        }
    }
}
