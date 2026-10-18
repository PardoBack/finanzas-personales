package com.example.appfinanzas.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.appfinanzas.data.repository.SecurityRepository
import com.example.appfinanzas.navigation.FinanceNavKey
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.screens.AddTransactionDialog
import com.example.appfinanzas.ui.screens.AiAssistantScreen
import com.example.appfinanzas.ui.screens.AnalyticsScreen
import com.example.appfinanzas.ui.screens.BudgetsScreen
import com.example.appfinanzas.ui.screens.DashboardScreen
import com.example.appfinanzas.ui.screens.FixedExpensesScreen
import com.example.appfinanzas.ui.screens.MovementsScreen
import com.example.appfinanzas.ui.screens.NotificationsDialog
import com.example.appfinanzas.ui.screens.OnboardingScreen
import com.example.appfinanzas.ui.screens.ProfileDialog
import com.example.appfinanzas.ui.screens.SavingsGoalsScreen
import com.example.appfinanzas.ui.screens.SecurityLockScreen
import com.example.appfinanzas.ui.screens.SecuritySettingsDialog
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.ui.viewmodel.AiAssistantViewModel
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.ui.viewmodel.FinancialViewModel

@Composable
fun FinanceApp(
    securityRepository: SecurityRepository? = null
) {
    val financialViewModel: FinancialViewModel = hiltViewModel()
    val financeViewModel: FinanceViewModel = hiltViewModel()

    val secRepo = securityRepository ?: financeViewModel.securityRepository

    val isFirstLaunch by financialViewModel.isFirstLaunch.collectAsState(initial = null)
    val isAppLocked by secRepo.isAppLocked.collectAsState()
    val isSecurityEnabled by secRepo.isSecurityEnabled.collectAsState(initial = false)
    val isBiometricEnabled by secRepo.isBiometricEnabled.collectAsState(initial = false)
    val pinCode by secRepo.pin.collectAsState(initial = "")

    val userProfile by financeViewModel.userProfileState.collectAsState()
    val notificationsState by financeViewModel.notificationsState.collectAsState()
    val syncState by financeViewModel.syncState.collectAsState()

    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showSecuritySettingsDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    // Loading state for preferences
    if (isFirstLaunch == null) return

    if (isAppLocked) {
        if (pinCode.isBlank() || !isSecurityEnabled) {
            secRepo.unlockApp()
        } else {
            SecurityLockScreen(
                onValidatePin = { secRepo.validatePin(it) },
                onSuccessUnlock = { secRepo.unlockApp() },
                onResetOrCreatePin = {
                    secRepo.unlockApp()
                    showSecuritySettingsDialog = true
                },
                isBiometricAvailable = secRepo.isBiometricSupported() && isBiometricEnabled
            )
            return
        }
    }

    val backStack = rememberNavBackStack(
        if (isFirstLaunch == true) FinanceNavKey.Onboarding else FinanceNavKey.Dashboard
    )

    val currentKey = backStack.last() as FinanceNavKey
    val showBottomBar = currentKey != FinanceNavKey.Onboarding

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            AnimatedVisibility(
                visible = showBottomBar,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                FinanceFloatingBottomBar(
                    currentKey = currentKey,
                    onNavigate = { key ->
                        if (backStack.last() != key) {
                            backStack.clear()
                            backStack.add(key)
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            NavDisplay<NavKey>(
                backStack = backStack,
                modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
                onBack = { backStack.removeLastOrNull() },
                transitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> fullWidth },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 3 },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                },
                popTransitionSpec = {
                    slideInHorizontally(
                        initialOffsetX = { fullWidth -> -fullWidth / 3 },
                        animationSpec = tween(300, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(300)) togetherWith
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(300, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(300))
                },
                entryProvider = { key ->
                    when (key) {
                        is FinanceNavKey.Onboarding -> NavEntry<NavKey>(key) {
                            OnboardingScreen(onFinish = {
                                financialViewModel.completeOnboarding()
                                backStack.clear()
                                backStack.add(FinanceNavKey.Dashboard)
                            })
                        }
                        is FinanceNavKey.Dashboard -> NavEntry<NavKey>(key) {
                            DashboardScreen(
                                viewModel = financeViewModel,
                                onOpenAddTransaction = { _ ->
                                    showAddTransactionDialog = true
                                },
                                onAddAntExpense = { name, amount ->
                                    financeViewModel.addAntExpense(name, amount)
                                },
                                onNavigateToMovements = {
                                    if (backStack.last() != FinanceNavKey.Movements) {
                                        backStack.clear()
                                        backStack.add(FinanceNavKey.Movements)
                                    }
                                },
                                onNavigateToFixedExpenses = {
                                    if (backStack.last() != FinanceNavKey.FixedExpenses) {
                                        backStack.clear()
                                        backStack.add(FinanceNavKey.FixedExpenses)
                                    }
                                },
                                onNavigateToBudgets = {
                                    if (backStack.last() != FinanceNavKey.Budgets) {
                                        backStack.clear()
                                        backStack.add(FinanceNavKey.Budgets)
                                    }
                                },
                                onNavigateToAiAssistant = {
                                    if (backStack.last() != FinanceNavKey.AiAssistant) {
                                        backStack.clear()
                                        backStack.add(FinanceNavKey.AiAssistant)
                                    }
                                },
                                onOpenSecuritySettings = {
                                    showSecuritySettingsDialog = true
                                },
                                onOpenProfile = {
                                    showProfileDialog = true
                                },
                                onOpenNotifications = {
                                    showNotificationsDialog = true
                                }
                            )
                        }
                        is FinanceNavKey.Movements -> NavEntry<NavKey>(key) {
                            MovementsScreen(viewModel = financeViewModel)
                        }
                        is FinanceNavKey.Budgets -> NavEntry<NavKey>(key) {
                            BudgetsScreen(
                                viewModel = financeViewModel,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                        is FinanceNavKey.FixedExpenses -> NavEntry<NavKey>(key) {
                            FixedExpensesScreen(
                                viewModel = financeViewModel,
                                onMarkAsPaid = { gastoFijo ->
                                    financeViewModel.payFixedExpense(gastoFijo)
                                }
                            )
                        }
                        is FinanceNavKey.Analytics -> NavEntry<NavKey>(key) {
                            AnalyticsScreen(
                                viewModel = financeViewModel,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                        is FinanceNavKey.Goals -> NavEntry<NavKey>(key) {
                            SavingsGoalsScreen(
                                viewModel = financeViewModel,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                        is FinanceNavKey.AiAssistant -> NavEntry<NavKey>(key) {
                            val aiViewModel: AiAssistantViewModel = hiltViewModel()
                            AiAssistantScreen(
                                viewModel = aiViewModel,
                                onBack = { backStack.removeLastOrNull() }
                            )
                        }
                        else -> error("Unknown key: $key")
                    }
                }
            )
        }

        if (showAddTransactionDialog) {
            AddTransactionDialog(
                viewModel = financeViewModel,
                onDismiss = { showAddTransactionDialog = false }
            )
        }

        val coroutineScope = rememberCoroutineScope()

        if (showSecuritySettingsDialog) {
            SecuritySettingsDialog(
                isSecurityEnabled = isSecurityEnabled,
                isBiometricEnabled = isBiometricEnabled,
                isBiometricSupported = secRepo.isBiometricSupported(),
                onToggleSecurity = { enabled ->
                    coroutineScope.launch {
                        secRepo.setSecurityEnabled(enabled)
                    }
                },
                onToggleBiometric = { enabled ->
                    coroutineScope.launch {
                        secRepo.setBiometricEnabled(enabled)
                    }
                },
                onValidateCurrentPin = { pin -> secRepo.validatePin(pin) },
                onChangePin = { newPin -> secRepo.setPin(newPin) },
                onDismiss = { showSecuritySettingsDialog = false }
            )
        }

        if (showNotificationsDialog) {
            NotificationsDialog(
                notifications = notificationsState.notifications,
                unreadCount = notificationsState.unreadCount,
                onDismissRequest = { showNotificationsDialog = false },
                onDismissNotification = { notifId -> financeViewModel.dismissNotification(notifId) },
                onClearAll = { financeViewModel.clearAllNotifications() },
                onNotificationAction = { actionType, _, _ ->
                    when (actionType) {
                        "PAY_FIXED" -> {
                            if (backStack.last() != FinanceNavKey.FixedExpenses) {
                                backStack.clear()
                                backStack.add(FinanceNavKey.FixedExpenses)
                            }
                        }
                        "NAVIGATE_BUDGETS" -> {
                            if (backStack.last() != FinanceNavKey.Budgets) {
                                backStack.clear()
                                backStack.add(FinanceNavKey.Budgets)
                            }
                        }
                        "NAVIGATE_AI" -> {
                            if (backStack.last() != FinanceNavKey.AiAssistant) {
                                backStack.clear()
                                backStack.add(FinanceNavKey.AiAssistant)
                            }
                        }
                        "NAVIGATE_MOVEMENTS" -> {
                            if (backStack.last() != FinanceNavKey.Movements) {
                                backStack.clear()
                                backStack.add(FinanceNavKey.Movements)
                            }
                        }
                    }
                }
            )
        }

        if (showProfileDialog) {
            ProfileDialog(
                userProfile = userProfile,
                syncState = syncState,
                appsScriptTemplate = financeViewModel.googleDriveSyncRepository.getAppsScriptTemplateCode(),
                onDismissRequest = { showProfileDialog = false },
                onSaveProfile = { name, currency, salary, webAppUrl, geminiKey ->
                    financeViewModel.updateUserProfile(name, currency, salary, webAppUrl, geminiKey)
                },
                onSyncWithDrive = {
                    financeViewModel.syncWithGoogleDrive()
                },
                onOpenSecuritySettings = {
                    showSecuritySettingsDialog = true
                }
            )
        }
    }
}

@Composable
fun FinanceFloatingBottomBar(
    currentKey: FinanceNavKey,
    onNavigate: (FinanceNavKey) -> Unit
) {
    val items = remember {
        listOf(
            Triple(FinanceNavKey.Dashboard, Icons.Rounded.Dashboard, "Resumen"),
            Triple(FinanceNavKey.Movements, Icons.AutoMirrored.Rounded.ListAlt, "Movs"),
            Triple(FinanceNavKey.Budgets, Icons.Rounded.PieChart, "Presupuestos"),
            Triple(FinanceNavKey.FixedExpenses, Icons.Rounded.Schedule, "Fijos"),
            Triple(FinanceNavKey.Analytics, Icons.Rounded.BarChart, "Análisis"),
            Triple(FinanceNavKey.Goals, Icons.Rounded.Savings, "Metas"),
            Triple(FinanceNavKey.AiAssistant, Icons.Rounded.SmartToy, "IA Pardo")
        )
    }

    val selectedIndex = when (currentKey) {
        FinanceNavKey.Dashboard -> 0
        FinanceNavKey.Movements -> 1
        FinanceNavKey.Budgets -> 2
        FinanceNavKey.FixedExpenses -> 3
        FinanceNavKey.Analytics -> 4
        FinanceNavKey.Goals -> 5
        FinanceNavKey.AiAssistant -> 6
        else -> 0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(32.dp))
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Emerald500.copy(alpha = 0.5f),
                            GlassBorder,
                            Violet500.copy(alpha = 0.5f)
                        )
                    ),
                    shape = RoundedCornerShape(32.dp)
                ),
            shape = RoundedCornerShape(32.dp),
            color = Slate900.copy(alpha = 0.94f),
            shadowElevation = 16.dp
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                val tabWidth = maxWidth / items.size
                val animatedOffsetX by animateDpAsState(
                    targetValue = tabWidth * selectedIndex,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "neonLightTracker"
                )

                // Neon lighting tracker pill smoothly following the active icon
                Box(
                    modifier = Modifier
                        .offset(x = animatedOffsetX)
                        .width(tabWidth)
                        .height(52.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Emerald500.copy(alpha = 0.35f),
                                    Violet500.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Emerald500,
                                    Violet500
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                )

                // Navigation Items Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items.forEachIndexed { index, (key, icon, label) ->
                        BottomNavItem(
                            modifier = Modifier.width(tabWidth),
                            selected = index == selectedIndex,
                            icon = icon,
                            label = label,
                            onClick = { onNavigate(key) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavItem(
    modifier: Modifier = Modifier,
    selected: Boolean,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "navScale"
    )

    val iconColor by animateColorAsState(
        targetValue = if (selected) Emerald500 else Slate400,
        animationSpec = tween(300),
        label = "iconColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .height(52.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .bounceClick(scaleDown = 0.90f) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = iconColor,
            maxLines = 1
        )
    }
}
