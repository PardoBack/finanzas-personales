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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
                    text = "Estadísticas y Análisis",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Visualización en vivo de tu flujo de dinero",
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
                    text = "2026",
                    color = AppTheme.colors.cyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }

        // 3 Tarjetas KPI principales estilo Dribbble
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Superávit
            Surface(
                modifier = Modifier.weight(1f),
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
                modifier = Modifier.weight(1f),
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
                modifier = Modifier.weight(1f),
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

        // Gráfico de Barras: Comparativa Mensual Ingresos vs Gastos
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

        // Distribución de Gastos por Categoría
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
                    text = "Distribución del consumo según registros reales",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

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
