package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appfinanzas.domain.model.FinancialNotification
import com.example.appfinanzas.domain.model.NotificationPriority
import com.example.appfinanzas.domain.model.NotificationType
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.AmberAlert
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500

@Composable
fun NotificationsDialog(
    notifications: List<FinancialNotification>,
    unreadCount: Int,
    onDismissRequest: () -> Unit,
    onDismissNotification: (String) -> Unit,
    onClearAll: () -> Unit,
    onNotificationAction: (actionType: String?, targetId: String?, targetCategory: String?) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
            color = Slate900
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background ambient glow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Violet500.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Emerald500.copy(alpha = 0.08f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                color = Violet500.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Violet500.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsActive,
                                        contentDescription = null,
                                        tint = Violet500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Centro de Alertas",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (unreadCount > 0) "$unreadCount alerta(s) activa(s)" else "Sin alertas pendientes",
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismissRequest()
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notifications list or empty state
                    if (notifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    color = Emerald500.copy(alpha = 0.15f),
                                    shape = CircleShape,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.4f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier
                                            .padding(20.dp)
                                            .size(48.dp)
                                    )
                                }
                                Text(
                                    text = "🎉 ¡Todo al día!",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "No tienes cuotas próximas a vencer ni excesos de presupuesto.",
                                    fontSize = 13.sp,
                                    color = Slate400,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 24.dp)
                                )
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onClearAll()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeleteOutline,
                                    contentDescription = null,
                                    tint = RedAlert,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Limpiar todo",
                                    fontSize = 12.sp,
                                    color = RedAlert,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 12.dp)
                        ) {
                            items(notifications, key = { it.id }) { notif ->
                                NotificationCardItem(
                                    notification = notif,
                                    onDismiss = { onDismissNotification(notif.id) },
                                    onActionClick = {
                                        onNotificationAction(notif.actionType, notif.targetId, notif.targetCategory)
                                        onDismissRequest()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate800,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Entendido",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCardItem(
    notification: FinancialNotification,
    onDismiss: () -> Unit,
    onActionClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val (icon, iconColor, cardBorderColor) = when (notification.type) {
        NotificationType.FIXED_EXPENSE_DUE -> Triple(Icons.Rounded.Payments, RedAlert, RedAlert.copy(alpha = 0.5f))
        NotificationType.BUDGET_OVERBUDGET -> Triple(Icons.Rounded.PieChart, AmberAlert, AmberAlert.copy(alpha = 0.5f))
        NotificationType.ANT_EXPENSE_ALERT -> Triple(Icons.Rounded.BugReport, AmberAlert, AmberAlert.copy(alpha = 0.5f))
        NotificationType.AI_RECOMMENDATION -> Triple(Icons.Rounded.SmartToy, Violet500, Violet500.copy(alpha = 0.5f))
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Slate800.copy(alpha = 0.85f),
        borderColor = cardBorderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = iconColor.copy(alpha = 0.18f),
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = notification.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = notification.dateText,
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    },
                    modifier = Modifier
                        .size(28.dp)
                        .bounceClick()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Descartar",
                        tint = Slate400,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = notification.message,
                fontSize = 12.sp,
                color = Slate200,
                lineHeight = 17.sp
            )

            if (notification.actionType != null) {
                Spacer(modifier = Modifier.height(10.dp))

                val actionBtnLabel = when (notification.actionType) {
                    "PAY_FIXED" -> "Pagar cuota ahora"
                    "NAVIGATE_BUDGETS" -> "Ajustar presupuestos"
                    "NAVIGATE_AI" -> "Consultar a la IA Pardo"
                    else -> "Ver detalles"
                }

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onActionClick()
                    },
                    modifier = Modifier.align(Alignment.End),
                    shape = RoundedCornerShape(10.dp),
                    color = iconColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, iconColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = actionBtnLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = iconColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
