package com.example.appfinanzas.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.SpatialGlowCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.AmberAlert
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.ui.viewmodel.CategoryAnalyticItem
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils
import com.example.appfinanzas.util.FinancialReportExporter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.analyticsState.collectAsState()
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val movementsState by viewModel.movementsState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val modelProducer = remember { CartesianChartModelProducer() }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(state.expensesByCategory) {
        if (state.expensesByCategory.isNotEmpty()) {
            modelProducer.runTransaction {
                columnSeries { series(state.expensesByCategory.values.map { it.toFloat() }) }
            }
        }
    }

    val comparisonModelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(state.monthlyComparison) {
        if (state.monthlyComparison.isNotEmpty()) {
            comparisonModelProducer.runTransaction {
                columnSeries {
                    series(state.monthlyComparison.values.map { it.first.toFloat() })
                    series(state.monthlyComparison.values.map { it.second.toFloat() })
                }
            }
        }
    }

    val periods = remember { listOf("Este Mes", "Últimos 3 Meses", "Año Actual") }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Analítica Avanzada", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        Text("Finanzas & IA Pardo", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onBack()
                        },
                        modifier = Modifier.bounceClick(scaleDown = 0.9f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = Slate200)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Selector de Período
            item {
                PeriodFilterRow(
                    selectedPeriod = state.selectedPeriod,
                    periods = periods,
                    onPeriodSelected = { period ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setAnalyticsPeriod(period)
                    }
                )
            }

            // 2. Score de Salud Financiera (0 - 100) Regla 50/30/20
            item {
                FinancialHealthScoreCard(
                    score = state.healthScore,
                    label = state.healthScoreLabel,
                    description = state.healthScoreDescription,
                    fixedRatio = state.fixedExpenseRatio,
                    lifestyleRatio = state.lifestyleRatio,
                    savingsRatio = state.savingsRatio
                )
            }

            // 3. Promedio de Gasto Diario & Proyección a Fin de Mes
            item {
                DailyAverageAndProjectionCard(
                    dailyAverage = state.dailyAverageExpense,
                    monthEndSpent = state.monthEndProjectedSpent,
                    monthEndBalance = state.monthEndProjectedBalance,
                    remainingFixed = state.remainingFixedExpenses,
                    daysPassed = state.daysPassed,
                    remainingDays = state.remainingDaysInMonth
                )
            }

            // 4. Indicador de Fuga Hormiga
            item {
                AntExpenseLeakCard(
                    totalAntExpense = state.antExpensesTotal,
                    incomePercentage = state.antExpensesPercentageOfIncome,
                    totalIncome = state.totalIncome
                )
            }

            // 5. Desglose de Categorías
            item {
                CategoryBreakdownSection(
                    categories = state.categoryBreakdown,
                    totalExpenses = state.totalExpenses
                )
            }

            // 6. Report Export CSV Tool
            item {
                ExportReportCard(
                    onExportCsv = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val report = FinancialReportExporter.generateCsvReport(
                            movimientos = movementsState.movimientos,
                            gastosFijos = dashboardState.gastosFijos,
                            totalBalance = dashboardState.totalBalance,
                            totalIngresos = dashboardState.monthlyIncome,
                            totalGastos = dashboardState.monthlyExpenses
                        )
                        FinancialReportExporter.shareReport(context, report)
                    }
                )
            }

            // 7. Vico Cartesian Charts
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Slate900.copy(alpha = 0.85f),
                    borderColor = GlassBorder
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PieChart, contentDescription = null, tint = Emerald500)
                        Text(
                            text = "Gastos por Categoría (Gráfico)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.expensesByCategory.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier.height(200.dp)
                        )
                    } else {
                        Text("No hay datos de gastos en este período", color = Slate400, fontSize = 13.sp)
                    }
                }
            }

            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    backgroundColor = Slate900.copy(alpha = 0.85f),
                    borderColor = GlassBorder
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, tint = Violet500)
                        Text(
                            text = "Comparativa Mensual Ingresos vs Gastos",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Text(
                        text = "Verde: Ingresos • Rojo: Gastos",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.monthlyComparison.isNotEmpty()) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis = rememberStartAxis(),
                                bottomAxis = rememberBottomAxis(),
                            ),
                            modelProducer = comparisonModelProducer,
                            modifier = Modifier.height(210.dp)
                        )
                    } else {
                        Text("No hay suficientes datos comparativos", color = Slate400, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PeriodFilterRow(
    selectedPeriod: String,
    periods: List<String>,
    onPeriodSelected: (String) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        items(periods) { period ->
            val isSelected = period == selectedPeriod
            Surface(
                onClick = { onPeriodSelected(period) },
                shape = RoundedCornerShape(20.dp),
                color = if (isSelected) Emerald500 else Slate900.copy(alpha = 0.8f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isSelected) Emerald500 else GlassBorder
                ),
                modifier = Modifier.bounceClick(scaleDown = 0.94f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CalendarToday,
                        contentDescription = null,
                        tint = if (isSelected) Color.White else Slate400,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = period,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color.White else Slate200
                    )
                }
            }
        }
    }
}

@Composable
fun FinancialHealthScoreCard(
    score: Int,
    label: String,
    description: String,
    fixedRatio: Double,
    lifestyleRatio: Double,
    savingsRatio: Double
) {
    val animatedProgress by animateFloatAsState(
        targetValue = (score / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "scoreProgress"
    )

    val scoreColor = when {
        score >= 80 -> Emerald500
        score >= 60 -> Color(0xFF38BDF8)
        score >= 40 -> AmberAlert
        else -> RedAlert
    }

    SpatialGlowCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 28.dp
    ) {
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            backgroundColor = Slate900.copy(alpha = 0.9f),
            borderColor = scoreColor.copy(alpha = 0.45f)
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
                        Icon(Icons.Rounded.Speed, contentDescription = null, tint = scoreColor)
                        Text(
                            text = "Score de Salud Financiera",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Surface(
                        color = scoreColor.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, scoreColor.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = scoreColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Score Gauge
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(100.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { 1f },
                            modifier = Modifier.fillMaxSize(),
                            color = Slate800,
                            strokeWidth = 10.dp,
                        )
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = scoreColor,
                            strokeWidth = 10.dp,
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$score",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Text(
                                text = "/100 pts",
                                fontSize = 10.sp,
                                color = Slate400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Explanation
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = description,
                            fontSize = 12.sp,
                            color = Slate200,
                            lineHeight = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Evaluado bajo la Regla 50/30/20",
                            fontSize = 10.sp,
                            color = Slate400,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Breakdown 50/30/20 bars
                Text(
                    text = "DESGLOSE DE ESTRUCTURA 50/30/20",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )
                Spacer(modifier = Modifier.height(8.dp))

                RuleRatioBar(
                    label = "Gastos Fijos / Necesidades",
                    currentRatio = fixedRatio,
                    targetText = "Meta <= 50%",
                    color = if (fixedRatio <= 50.0) Emerald500 else AmberAlert
                )
                Spacer(modifier = Modifier.height(6.dp))

                RuleRatioBar(
                    label = "Estilo de Vida / Gustos",
                    currentRatio = lifestyleRatio,
                    targetText = "Meta <= 30%",
                    color = if (lifestyleRatio <= 30.0) Violet500 else RedAlert
                )
                Spacer(modifier = Modifier.height(6.dp))

                RuleRatioBar(
                    label = "Ahorro / Superávit",
                    currentRatio = savingsRatio,
                    targetText = "Meta >= 20%",
                    color = if (savingsRatio >= 20.0) Emerald500 else AmberAlert
                )
            }
        }
    }
}

@Composable
fun RuleRatioBar(
    label: String,
    currentRatio: Double,
    targetText: String,
    color: Color
) {
    val animatedValue by animateFloatAsState(
        targetValue = (currentRatio / 100.0).toFloat().coerceIn(0f, 1f),
        animationSpec = tween(700),
        label = "ratioProgress"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium)
            Text(
                text = "${String.format(Locale.getDefault(), "%.1f", currentRatio)}% ($targetText)",
                fontSize = 10.sp,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        LinearProgressIndicator(
            progress = { animatedValue },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = color,
            trackColor = Slate800
        )
    }
}

@Composable
fun DailyAverageAndProjectionCard(
    dailyAverage: Double,
    monthEndSpent: Double,
    monthEndBalance: Double,
    remainingFixed: Double,
    daysPassed: Int,
    remainingDays: Int
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = GlassBorder
    ) {
        Column {
            Text(
                text = "Promedio Diario & Proyección Cierre",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Basado en $daysPassed días transcurridos y $remainingDays días restantes",
                fontSize = 11.sp,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Promedio Diario
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Slate800.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Promedio Diario", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatCOP(dailyAverage),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text("/ día gastado", fontSize = 9.sp, color = Emerald500)
                }

                // Proyección Cierre Gasto
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Slate800.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Text("Gasto Proyectado Mes", fontSize = 11.sp, color = Slate400, fontWeight = FontWeight.Medium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = CurrencyUtils.formatCOP(monthEndSpent),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = AmberAlert
                    )
                    Text("al cierre estimado", fontSize = 9.sp, color = Slate400)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // SubCard for Projected Balance
            Surface(
                color = if (monthEndBalance >= 0) Emerald500.copy(alpha = 0.12f) else RedAlert.copy(alpha = 0.12f),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (monthEndBalance >= 0) Emerald500.copy(alpha = 0.4f) else RedAlert.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Saldo Proyectado Cierre de Mes",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Incluye ${CurrencyUtils.formatCOP(remainingFixed)} de fijos pendientes",
                            fontSize = 10.sp,
                            color = Slate400
                        )
                    }
                    Text(
                        text = CurrencyUtils.formatCOP(monthEndBalance),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = if (monthEndBalance >= 0) Emerald500 else RedAlert
                    )
                }
            }
        }
    }
}

@Composable
fun AntExpenseLeakCard(
    totalAntExpense: Double,
    incomePercentage: Double,
    totalIncome: Double
) {
    val isHighLeak = incomePercentage > 5.0
    val annualLeak = if (totalAntExpense > 0) totalAntExpense * 12 else 0.0

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = if (isHighLeak) RedAlert.copy(alpha = 0.10f) else Slate900.copy(alpha = 0.85f),
        borderColor = if (isHighLeak) RedAlert.copy(alpha = 0.5f) else Violet500.copy(alpha = 0.4f)
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
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Violet500.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.BugReport, contentDescription = null, tint = Violet500)
                    }
                    Column {
                        Text(
                            text = "Indicador de Fuga Hormiga",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Microcompras y gustos impulsivos",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Surface(
                    color = if (isHighLeak) RedAlert.copy(alpha = 0.2f) else Emerald500.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "${String.format(Locale.getDefault(), "%.1f", incomePercentage)}% de tus ingresos",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isHighLeak) RedAlert else Emerald500
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Hormiga del Período", fontSize = 11.sp, color = Slate400)
                    Text(
                        text = CurrencyUtils.formatCOP(totalAntExpense),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(32.dp)
                        .background(Slate800)
                )

                Column {
                    Text("Impacto Anual Estimado", fontSize = 11.sp, color = Slate400)
                    Text(
                        text = CurrencyUtils.formatCOP(annualLeak),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberAlert
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isHighLeak) {
                    "🚨 ALERTA: Tus gastos hormiga superan el 5% de tus ingresos. Si reduces la mitad de estos microgastos, podrías abonar ${CurrencyUtils.formatCOP(annualLeak / 2)} al año a tus metas."
                } else {
                    "✅ Buen control de gastos hormiga. Mantén tu semáforo de microcompras activado en el Dashboard."
                },
                fontSize = 11.sp,
                color = Slate200,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CategoryBreakdownSection(
    categories: List<CategoryAnalyticItem>,
    totalExpenses: Double
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = GlassBorder
    ) {
        Column {
            Text(
                text = "Desglose de Categorías",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Porcentaje del total gastado (${CurrencyUtils.formatCOP(totalExpenses)})",
                fontSize = 11.sp,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (categories.isEmpty()) {
                Text("No hay gastos registrados en este período", fontSize = 13.sp, color = Slate400)
            } else {
                categories.forEachIndexed { index, item ->
                    val color = when (index % 4) {
                        0 -> Emerald500
                        1 -> Violet500
                        2 -> AmberAlert
                        else -> RedAlert
                    }

                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Text(
                                    text = item.category,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "(${item.count} movs)",
                                    fontSize = 10.sp,
                                    color = Slate400
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%.1f", item.percentage)}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = color
                                )
                                Text(
                                    text = CurrencyUtils.formatCOP(item.amount),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        val animatedVal by animateFloatAsState(
                            targetValue = (item.percentage / 100.0).toFloat().coerceIn(0f, 1f),
                            animationSpec = tween(600),
                            label = "catProg"
                        )

                        LinearProgressIndicator(
                            progress = { animatedVal },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = color,
                            trackColor = Slate800
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExportReportCard(onExportCsv: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        backgroundColor = Slate900.copy(alpha = 0.9f),
        borderColor = Emerald500.copy(alpha = 0.35f)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = null,
                    tint = Emerald500
                )
                Text(
                    text = "Exportación de Reporte Financiero",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Genera tu reporte contable completo en formato CSV para abrir en Excel o compartir.",
                fontSize = 12.sp,
                color = Slate400
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onExportCsv,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick(scaleDown = 0.94f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Exportar Reporte Financiero (CSV)",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
