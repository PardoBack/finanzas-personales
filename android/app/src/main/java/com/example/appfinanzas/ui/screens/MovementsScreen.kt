package com.example.appfinanzas.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.components.shimmerEffect
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovementsScreen(viewModel: FinanceViewModel) {
    val uiState by viewModel.movementsState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

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
                            "Historial FinTech",
                            style = MaterialTheme.typography.titleMedium,
                            color = Emerald500,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Movimientos",
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
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Real-Time Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = topPadding)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = {
                        Text(
                            "Buscar por descripción, categoría, comercio o cuenta...",
                            fontSize = 12.sp,
                            color = Slate400
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Buscar",
                            tint = Emerald500
                        )
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setSearchQuery("")
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Clear,
                                    contentDescription = "Limpiar búsqueda",
                                    tint = Slate400
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = GlassBorder,
                        focusedContainerColor = Slate900.copy(alpha = 0.85f),
                        unfocusedContainerColor = Slate900.copy(alpha = 0.85f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            FilterChips(
                selectedFilter = uiState.filterType,
                onFilterSelected = { viewModel.setMovementsFilter(it) },
                modifier = Modifier.padding(top = 2.dp)
            )

            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = { viewModel.refreshAllData() },
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState.isLoading && !uiState.isRefreshing) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(5) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(72.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .shimmerEffect()
                            )
                        }
                    }
                } else {
                    val movimientos = uiState.movimientos

                    if (movimientos.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (uiState.searchQuery.isNotBlank())
                                        "No se encontraron movimientos para \"${uiState.searchQuery}\""
                                    else
                                        "No hay movimientos para este filtro",
                                    color = Slate400,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 20.dp, end = 20.dp,
                                top = 8.dp, bottom = paddingValues.calculateBottomPadding() + 16.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(movimientos) { movimiento ->
                                DetailedMovementItem(movimiento)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FilterChips(
    selectedFilter: String,
    onFilterSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        listOf("Todos", "Gastos", "Ingresos").forEach { filter ->
            val selected = selectedFilter == filter
            FilterChip(
                modifier = Modifier.bounceClick(scaleDown = 0.94f) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterSelected(filter)
                },
                selected = selected,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onFilterSelected(filter)
                },
                label = {
                    Text(
                        filter,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                    selectedLabelColor = Emerald500,
                    containerColor = Slate900,
                    labelColor = Slate400
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected,
                    selectedBorderColor = Emerald500,
                    borderColor = GlassBorder
                ),
                shape = RoundedCornerShape(14.dp)
            )
        }
    }
}

@Composable
fun DetailedMovementItem(movimiento: Movimiento) {
    val isIngreso = movimiento.tipo == "ingreso"

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = if (isIngreso) Emerald500.copy(alpha = 0.25f) else GlassBorder
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
                            if (isIngreso) Emerald500.copy(alpha = 0.15f) else RedAlert.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(movimiento.categoria),
                        contentDescription = null,
                        tint = if (isIngreso) Emerald500 else RedAlert,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column {
                    Text(
                        movimiento.concepto,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "${movimiento.categoria} • ${movimiento.cuenta} • ${movimiento.fecha}",
                        fontSize = 11.sp,
                        color = Slate400
                    )
                    if (!movimiento.observaciones.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Surface(
                            color = Violet500.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                movimiento.observaciones.orEmpty(),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Violet500
                            )
                        }
                    }
                }
            }

            Text(
                (if (isIngreso) "+ " else "- ") + CurrencyUtils.formatCOP(movimiento.monto.toDouble()),
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = if (isIngreso) Emerald500 else RedAlert
            )
        }
    }
}

fun getCategoryIcon(categoria: String): ImageVector {
    return when (categoria) {
        "Alimentación" -> Icons.Rounded.Restaurant
        "Transporte" -> Icons.Rounded.DirectionsCar
        "Vivienda" -> Icons.Rounded.Home
        "Servicios" -> Icons.AutoMirrored.Rounded.ReceiptLong
        "Salud" -> Icons.Rounded.MedicalServices
        "Entretenimiento" -> Icons.Rounded.SportsEsports
        "Sueldo" -> Icons.Rounded.Payments
        "Ocio" -> Icons.Rounded.Celebration
        else -> Icons.Rounded.Category
    }
}
