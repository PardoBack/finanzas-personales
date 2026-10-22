package com.example.finanzaspersonales.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.viewmodel.DashboardUiState
import com.example.finanzaspersonales.viewmodel.MainViewModel

@Composable
fun EstadisticasSection(uiState: DashboardUiState) {
    val scrollState = rememberScrollState()

    // Cálculo dinámico de gastos por categoría
    val gastosMovs = remember(uiState.movimientos) {
        uiState.movimientos.filter { it.tipo.equals("Gasto", ignoreCase = true) }
    }

    val categoriasMap = remember(gastosMovs) {
        gastosMovs.groupBy { it.categoria.ifBlank { "Varios" } }
            .mapValues { entry -> entry.value.sumOf { it.valor } }
            .toList()
            .sortedByDescending { it.second }
    }

    val totalGastos = remember(categoriasMap) {
        categoriasMap.sumOf { it.second }.coerceAtLeast(1L)
    }

    val mayorGasto = categoriasMap.firstOrNull()
    val mayorGastoPct = if (mayorGasto != null) ((mayorGasto.second.toDouble() / totalGastos.toDouble()) * 100).toInt() else 0

    // Métricas de Burn Rate y Proyecciones
    val gastoDiarioPromedio = remember(uiState.gastosMes) {
        (uiState.gastosMes / 30L).coerceAtLeast(1L)
    }
    val diasCobertura = remember(uiState.disponibleReal, gastoDiarioPromedio) {
        (uiState.disponibleReal / gastoDiarioPromedio).coerceIn(0L, 365L)
    }
    val fijosSobreIngresoPct = remember(uiState.totalCuotasFijos, uiState.ingresosMes) {
        if (uiState.ingresosMes > 0) ((uiState.totalCuotasFijos.toDouble() / uiState.ingresosMes.toDouble()) * 100).toInt() else 0
    }
    val remanenteFinDeMes = remember(uiState.disponibleReal, uiState.fijosPendientes) {
        uiState.disponibleReal - uiState.fijosPendientes
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de la sección
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Estadísticas y Análisis Pro",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "Métricas de consumo, alertas y proyecciones",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp
                )
            }
            Surface(
                color = AppTheme.colors.cyan.copy(alpha = 0.15f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AppTheme.colors.cyan.copy(alpha = 0.35f))
            ) {
                Text(
                    text = "En Vivo",
                    color = AppTheme.colors.cyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }

        // 1. Banner Inteligente de Alerta de Consumo ("Gastando mucho en esto")
        if (mayorGasto != null) {
            val esGastoCritico = mayorGastoPct >= 35
            val alertBorder = if (esGastoCritico) AppTheme.colors.amber else AppTheme.colors.cyan
            val alertBg = if (esGastoCritico) AppTheme.colors.amber.copy(alpha = 0.12f) else AppTheme.colors.cyan.copy(alpha = 0.10f)

            Surface(
                color = alertBg,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, alertBorder.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .pressScale(0.98f)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(alertBorder.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (esGastoCritico) Icons.Default.WarningAmber else Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = alertBorder,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (esGastoCritico) "Mayor Centro de Gasto: ${mayorGasto.first}" else "Consumo Equilibrado",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = if (esGastoCritico) AppTheme.colors.rose.copy(alpha = 0.2f) else AppTheme.colors.emerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "$mayorGastoPct% del total",
                                    color = if (esGastoCritico) AppTheme.colors.rose else AppTheme.colors.emeraldBright,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (esGastoCritico)
                                "Estás gastando bastante en ${mayorGasto.first} ($${MainViewModel.formatearNumero(mayorGasto.second)} COP). Te sugerimos vigilar compras hormiga para proteger tu disponible."
                            else
                                "Tus gastos están repartidos de forma saludable entre categorías sin desviaciones de riesgo.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }

        // 2. Tablero 2x2 de Métricas Financieras Clave
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Indicadores de Salud Financiera",
                color = AppTheme.colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Métrica 1: Gasto Diario Promedio
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Gasto Diario Prom.",
                    value = "$${MainViewModel.formatearNumero(gastoDiarioPromedio)}",
                    subtitle = "Ritmo estimado",
                    color = AppTheme.colors.rose,
                    icon = Icons.Default.LocalFireDepartment
                )
                // Métrica 2: Runway / Días de Cobertura
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Días de Cobertura",
                    value = "$diasCobertura días",
                    subtitle = "Liquidez disponible",
                    color = AppTheme.colors.cyan,
                    icon = Icons.Default.Shield
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Métrica 3: Carga de Compromisos Fijos
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Carga Fijos / Ingreso",
                    value = "$fijosSobreIngresoPct%",
                    subtitle = if (fijosSobreIngresoPct < 50) "Rango saludable" else "Carga moderada",
                    color = AppTheme.colors.amber,
                    icon = Icons.Default.EventRepeat
                )
                // Métrica 4: Remanente Fin de Mes
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Proyección Cierre",
                    value = "$${MainViewModel.formatearNumero(remanenteFinDeMes)}",
                    subtitle = "Tras fijos pendientes",
                    color = AppTheme.colors.emeraldBright,
                    icon = Icons.Default.CheckCircle
                )
            }
        }

        // 3. Tarjetas KPI principales estilo Dribbble (Superávit, Ahorro, Fondos)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Superávit
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(0.97f),
                color = AppTheme.colors.cardBackground,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AppTheme.colors.subtleBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.cyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                            contentDescription = null,
                            tint = AppTheme.colors.cyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Superávit 2026",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${MainViewModel.formatearNumero(uiState.superavitAnual)}",
                        color = AppTheme.colors.cyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Tasa Ahorro
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(0.97f),
                color = AppTheme.colors.cardBackground,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AppTheme.colors.subtleBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.emerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Savings,
                            contentDescription = null,
                            tint = AppTheme.colors.emerald,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Tasa de Ahorro",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${uiState.tasaAhorroAnual}%",
                        color = AppTheme.colors.emeraldBright,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Fondos Colanta
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .pressScale(0.97f),
                color = AppTheme.colors.cardBackground,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, AppTheme.colors.subtleBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppTheme.colors.amber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AppTheme.colors.amber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Fondos Reserva",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$${MainViewModel.formatearNumero(uiState.totalFondosColanta)}",
                        color = AppTheme.colors.amber,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // 4. Gráfico Flujo de Dinero 2026 estilo Dribbble (Ingresos vs Gastos)
        Surface(
            color = AppTheme.colors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Flujo de Dinero 2026",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Ingresos vs Gastos mes a mes",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }

                    // Leyenda
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppTheme.colors.emerald))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ingresos", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(AppTheme.colors.rose))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Gastos", color = AppTheme.colors.textSecondary, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Gráfico scrollable con barras
                val maxVal = remember(uiState.resumenAnual) {
                    uiState.resumenAnual.maxOfOrNull { maxOf(it.ingresos, it.gastos) }?.coerceAtLeast(1000000L) ?: 4000000L
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    uiState.resumenAnual.forEach { mesItem ->
                        val isCurrent = mesItem.key.equals(uiState.mesActivo, ignoreCase = true)
                        val ingHeight = ((mesItem.ingresos.toFloat() / maxVal.toFloat()) * 110f).coerceIn(4f, 110f)
                        val gasHeight = ((mesItem.gastos.toFloat() / maxVal.toFloat()) * 110f).coerceIn(4f, 110f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(38.dp)
                        ) {
                            // Barras
                            Row(
                                modifier = Modifier.height(115.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Barra Ingreso
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(ingHeight.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(AppTheme.colors.emeraldBright, AppTheme.colors.emerald)
                                            )
                                        )
                                )
                                // Barra Gasto
                                Box(
                                    modifier = Modifier
                                        .width(14.dp)
                                        .height(gasHeight.dp)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(AppTheme.colors.roseSoft, AppTheme.colors.rose)
                                            )
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Etiqueta del mes
                            Surface(
                                color = if (isCurrent) AppTheme.colors.cyan.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = mesItem.nombre.take(3),
                                    color = if (isCurrent) AppTheme.colors.cyan else AppTheme.colors.textSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Distribución de Gastos por Categoría con Badges de Nivel de Consumo
        Surface(
            color = AppTheme.colors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Gastos por Categoría (${uiState.mesActivo.replaceFirstChar { it.uppercase() }})",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Distribución y semáforo de consumo",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (categoriasMap.isEmpty()) {
                    Text(
                        text = "No hay gastos registrados en este periodo.",
                        color = AppTheme.colors.textMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    val colors = listOf(
                        AppTheme.colors.rose,
                        AppTheme.colors.cyan,
                        AppTheme.colors.amber,
                        AppTheme.colors.violet,
                        AppTheme.colors.emerald
                    )

                    categoriasMap.take(6).forEachIndexed { idx, (cat, monto) ->
                        val pct = ((monto.toDouble() / totalGastos.toDouble()) * 100).toInt()
                        val color = colors[idx % colors.size]

                        val badgeLabel = when {
                            pct >= 30 -> "Elevado"
                            pct >= 15 -> "Moderado"
                            else -> "Controlado"
                        }
                        val badgeColor = when {
                            pct >= 30 -> AppTheme.colors.rose
                            pct >= 15 -> AppTheme.colors.amber
                            else -> AppTheme.colors.emeraldBright
                        }

                        Column(modifier = Modifier.padding(vertical = 6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = cat,
                                        color = AppTheme.colors.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = badgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = badgeLabel,
                                            color = badgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "$${MainViewModel.formatearNumero(monto)} ($pct%)",
                                    color = AppTheme.colors.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            // Barra de progreso
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AppTheme.colors.cardSurfaceLight)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(pct.toFloat() / 100f)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Surface(
        color = AppTheme.colors.cardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
        modifier = modifier.pressScale(0.97f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = AppTheme.colors.textMuted,
                fontSize = 10.sp
            )
        }
    }
}
