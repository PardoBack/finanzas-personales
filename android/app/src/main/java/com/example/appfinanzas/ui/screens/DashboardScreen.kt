package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LockClock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.data.BudgetCategory
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.QuickActionButton
import com.example.appfinanzas.ui.components.SmartInsightsBanner
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.components.shimmerEffect
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
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils

import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.SmartToy

@Composable
fun DashboardScreen(
    viewModel: FinanceViewModel,
    onOpenAddTransaction: (String) -> Unit = {},
    onAddAntExpense: (String, Double) -> Unit = { _, _ -> },
    onNavigateToMovements: () -> Unit = {},
    onNavigateToFixedExpenses: () -> Unit = {},
    onNavigateToBudgets: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {},
    onOpenSecuritySettings: () -> Unit = {},
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val uiState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfileState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    var isBalanceVisible by remember { mutableStateOf(true) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            DashboardTopBar(
                userName = userProfile.userName,
                unreadCount = notificationsState.unreadCount,
                onOpenProfile = onOpenProfile,
                onOpenNotifications = onOpenNotifications,
                onOpenSecuritySettings = onOpenSecuritySettings,
                onNavigateToAiAssistant = onNavigateToAiAssistant
            )
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        if (uiState.isLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(top = topPadding, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(28.dp)).shimmerEffect())
                Box(modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                    Box(modifier = Modifier.weight(1f).height(110.dp).clip(RoundedCornerShape(20.dp)).shimmerEffect())
                }
                Box(modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(24.dp)).shimmerEffect())
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = topPadding + 8.dp, bottom = padding.calculateBottomPadding() + 32.dp)
            ) {
                item {
                    HeroBalanceCard(
                        totalBalance = uiState.totalBalance,
                        disponibleReal = uiState.disponibleReal,
                        comprometidoFijos = uiState.comprometidoFijos,
                        isVisible = isBalanceVisible,
                        onToggleVisibility = { isBalanceVisible = !isBalanceVisible }
                    )
                }

                item {
                    QuickActionsSection(
                        onAddExpense = { onOpenAddTransaction("gasto") },
                        onAddIncome = { onOpenAddTransaction("ingreso") },
                        onAddAntExpenseClick = { onAddAntExpense("Café", 5000.0) }
                    )
                }

                item {
                    SmartInsightsBanner(
                        onActionClick = { insight ->
                            when (insight.id) {
                                1 -> onOpenAddTransaction("gasto")
                                2 -> onAddAntExpense("Café", 5000.0)
                                3 -> onOpenAddTransaction("ingreso")
                                4 -> onNavigateToFixedExpenses()
                                else -> onOpenAddTransaction("gasto")
                            }
                        }
                    )
                }

                item {
                    AnimatedVisibility(
                        visible = uiState.overspentCategories.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        BudgetAlertBanner(
                            overspentCategories = uiState.overspentCategories,
                            onClick = onNavigateToBudgets
                        )
                    }
                }

                item {
                    MetricsGrid(
                        income = uiState.monthlyIncome,
                        expenses = uiState.monthlyExpenses
                    )
                }

                // 6. Ant Expenses Section
                item {
                    AntExpensesSection(
                        currentDailyTotal = uiState.dailyAntTotal,
                        dailyLimit = uiState.dailyAntLimit,
                        onAddAntExpense = onAddAntExpense
                    )
                }

                // 7. Recent Movements
                item {
                    RecentMovementsSection(
                        movimientos = uiState.movimientos,
                        onViewAllClick = onNavigateToMovements
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardTopBar(
    userName: String = "Pro",
    unreadCount: Int = 0,
    onOpenProfile: () -> Unit = {},
    onOpenNotifications: () -> Unit = {},
    onOpenSecuritySettings: () -> Unit = {},
    onNavigateToAiAssistant: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current

    TopAppBar(
        modifier = Modifier.statusBarsPadding(),
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        ),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onOpenProfile()
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Emerald500.copy(alpha = 0.14f))
                        .border(1.dp, Emerald500.copy(alpha = 0.32f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Perfil",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Pardo Finanzas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400
                    )
                    Text(
                        text = "Hola, $userName 👋",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        color = Color.White
                    )
                }
            }
        },
        actions = {
            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onNavigateToAiAssistant()
                },
                modifier = Modifier.bounceClick(scaleDown = 0.9f)
            ) {
                Surface(
                    color = Violet500.copy(alpha = 0.12f),
                    shape = CircleShape,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Violet500.copy(alpha = 0.3f))
                ) {
                    Icon(Icons.Rounded.SmartToy, contentDescription = "Asistente financiero", tint = Violet500, modifier = Modifier.padding(8.dp).size(20.dp))
                }
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenSecuritySettings()
                },
                modifier = Modifier.bounceClick(scaleDown = 0.9f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = "Ajustes de Seguridad",
                    tint = Slate400
                )
            }

            IconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onOpenNotifications()
                },
                modifier = Modifier.bounceClick(scaleDown = 0.9f)
            ) {
                Box {
                    Icon(
                        Icons.Rounded.Notifications,
                        contentDescription = "Notificaciones",
                        tint = Slate200
                    )
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(RedAlert)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun HeroBalanceCard(
    totalBalance: Double,
    disponibleReal: Double,
    comprometidoFijos: Double,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Slate900,
        borderColor = GlassBorder
    ) {
        Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Emerald500.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "BALANCE TOTAL",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onToggleVisibility()
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = if (isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                            contentDescription = "Ocultar/Mostrar saldo",
                            tint = Slate400,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isVisible) CurrencyUtils.formatCOP(totalBalance) else "••••••••",
                    fontSize = 29.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.0).sp,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.Transparent, Slate700, Color.Transparent)
                            )
                        )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Disponible Real",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (isVisible) CurrencyUtils.formatCOP(disponibleReal) else "••••••",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = Emerald500
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(36.dp)
                            .background(Slate700)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LockClock,
                                contentDescription = null,
                                tint = AmberAlert,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Gastos fijos",
                                fontSize = 11.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = if (isVisible) CurrencyUtils.formatCOP(comprometidoFijos) else "••••••",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp,
                            color = AmberAlert
                        )
                    }
                }
        }
    }
}

@Composable
fun BudgetAlertBanner(
    overspentCategories: List<BudgetCategory>,
    onClick: () -> Unit = {}
) {
    val totalOverdraft = overspentCategories.sumOf { it.spentAmount - it.allocatedAmount }
    val haptic = LocalHapticFeedback.current

    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick(scaleDown = 0.96f) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        backgroundColor = RedAlert.copy(alpha = 0.12f),
        borderColor = RedAlert.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(RedAlert.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🚨", fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SOBREGIRO EN PRESUPUESTO",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    color = RedAlert
                )
                Text(
                    text = "${overspentCategories.size} categoría(s) excedida(s) por ${CurrencyUtils.formatCOP(totalOverdraft)}",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(
    onAddExpense: () -> Unit,
    onAddIncome: () -> Unit,
    onAddAntExpenseClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        QuickActionButton(
            icon = Icons.Rounded.RemoveCircleOutline,
            label = "Gasto",
            accentColor = RedAlert,
            onClick = onAddExpense,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Rounded.AddCircleOutline,
            label = "Ingreso",
            accentColor = Emerald500,
            onClick = onAddIncome,
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Rounded.BugReport,
            label = "Hormiga",
            accentColor = Violet500,
            onClick = onAddAntExpenseClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun MetricsGrid(income: Double, expenses: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Ingresos",
            amount = income,
            icon = Icons.Rounded.ArrowUpward,
            color = Emerald500
        )
        MetricCard(
            modifier = Modifier.weight(1f),
            title = "Gastos",
            amount = expenses,
            icon = Icons.Rounded.ArrowDownward,
            color = RedAlert
        )
    }
}

@Composable
fun MetricCard(
    modifier: Modifier,
    title: String,
    amount: Double,
    icon: ImageVector,
    color: Color,
) {
    GlassCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Slate900.copy(alpha = 0.8f),
        borderColor = color.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(17.dp))
            }
            Text(title, fontSize = 12.sp, color = Slate400, fontWeight = FontWeight.Medium, maxLines = 1)
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
            CurrencyUtils.formatCOP(amount),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
fun RecentMovementsSection(
    movimientos: List<Movimiento>,
    onViewAllClick: () -> Unit = {},
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Slate900.copy(alpha = 0.8f),
        borderColor = GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Movimientos Recientes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            TextButton(onClick = onViewAllClick) {
                Text("Ver todo", color = Emerald500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        movimientos.take(5).forEachIndexed { index, movimiento ->
            val isGasto = movimiento.tipo == "gasto"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isGasto) RedAlert.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isGasto) Icons.Rounded.Remove else Icons.Rounded.Add,
                        contentDescription = null,
                        tint = if (isGasto) RedAlert else Emerald500,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(movimiento.concepto, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                    Text(
                        "${movimiento.categoria} • ${movimiento.fecha}",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                }

                Text(
                    (if (isGasto) "- " else "+ ") + CurrencyUtils.formatCOP(movimiento.monto.toDouble()),
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    letterSpacing = (-0.3).sp,
                    color = if (isGasto) RedAlert else Emerald500
                )
            }

            if (index < movimientos.take(5).lastIndex) {
                HorizontalDivider(color = Slate800, thickness = 1.dp)
            }
        }
    }
}
