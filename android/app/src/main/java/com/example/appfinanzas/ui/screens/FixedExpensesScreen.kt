package com.example.appfinanzas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Payment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.StatusBadge
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.components.shimmerEffect
import com.example.appfinanzas.ui.theme.*
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixedExpensesScreen(
    viewModel: FinanceViewModel,
    onMarkAsPaid: (GastoFijo) -> Unit = {}
) {
    val uiState by viewModel.fixedExpensesState.collectAsStateWithLifecycle()
    val addTxState by viewModel.addTransactionState.collectAsStateWithLifecycle()

    var showPaymentDialog by remember { mutableStateOf(false) }

    LaunchedEffect(addTxState.prefill) {
        if (addTxState.prefill != null) {
            showPaymentDialog = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                ),
                title = {
                    Column {
                        Text(
                            text = "Control de Gastos Fijos",
                            style = MaterialTheme.typography.titleMedium,
                            color = Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Obligaciones Mensuales",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        val topPadding = paddingValues.calculateTopPadding()
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = { viewModel.refreshAllData() },
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && !uiState.isRefreshing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .padding(top = topPadding + 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    repeat(4) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .shimmerEffect()
                        )
                    }
                }
            } else if (uiState.gastosFijos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().padding(top = topPadding), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay gastos fijos configurados",
                        color = Slate400,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                val currentDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 20.dp, end = 20.dp,
                        top = topPadding + 16.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(uiState.gastosFijos) { gastoFijo ->
                        val isPaid = uiState.pagadosIds.contains(gastoFijo.id)
                        val status = when {
                            isPaid -> "PAGADO"
                            gastoFijo.diaPago < currentDay -> "VENCIDO"
                            else -> "PENDIENTE"
                        }

                        FixedExpenseItemCard(
                            gastoFijo = gastoFijo,
                            status = status,
                            isPaid = isPaid,
                            onPayClick = {
                                viewModel.prefillFixedExpense(gastoFijo)
                                onMarkAsPaid(gastoFijo)
                            }
                        )
                    }
                }
            }
        }

        if (showPaymentDialog) {
            AddTransactionDialog(
                viewModel = viewModel,
                onDismiss = {
                    showPaymentDialog = false
                    viewModel.clearPrefill()
                }
            )
        }
    }
}

@Composable
fun FixedExpenseItemCard(
    gastoFijo: GastoFijo,
    status: String,
    isPaid: Boolean,
    onPayClick: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = when (status) {
            "PAGADO" -> Emerald500.copy(alpha = 0.3f)
            "VENCIDO" -> RedAlert.copy(alpha = 0.5f)
            else -> GlassBorder
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            when (status) {
                                "PAGADO" -> Emerald500.copy(alpha = 0.15f)
                                "VENCIDO" -> RedAlert.copy(alpha = 0.15f)
                                else -> Violet500.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (status) {
                            "PAGADO" -> Icons.Rounded.CheckCircle
                            "VENCIDO" -> Icons.Rounded.Schedule
                            else -> Icons.Rounded.CalendarMonth
                        },
                        contentDescription = null,
                        tint = when (status) {
                            "PAGADO" -> Emerald500
                            "VENCIDO" -> RedAlert
                            else -> Violet500
                        },
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        text = gastoFijo.concepto,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Día de pago: ${gastoFijo.diaPago} de cada mes",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    StatusBadge(status = status)
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = CurrencyUtils.formatCOP(gastoFijo.monto),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = Color.White
                )

                if (!isPaid) {
                    val haptic = LocalHapticFeedback.current
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Emerald500, Emerald600)
                                )
                            )
                            .bounceClick {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onPayClick()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Payment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Pagar Cuota",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
