package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.data.BudgetCategory
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.AmberAlert
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit = {}
) {
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var editingCategory by remember { mutableStateOf<BudgetCategory?>(null) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }

    val categories = dashboardState.budgetCategories
    val totalAllocated = categories.sumOf { it.allocatedAmount }
    val totalSpent = categories.sumOf { it.spentAmount }
    val overspentCount = categories.count { it.spentAmount >= it.allocatedAmount }
    val warningCount = categories.count {
        val pct = if (it.allocatedAmount > 0) it.spentAmount / it.allocatedAmount else 0.0
        pct in 0.8..0.999
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text(
                            text = "Presupuestos & Alertas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Emerald500
                        )
                        Text(
                            text = "Límites por Categoría",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
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
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Volver",
                            tint = Slate200
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showNewCategoryDialog = true
                },
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.bounceClick(scaleDown = 0.92f)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Nuevo Presupuesto")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Summary Card
            item {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    backgroundColor = Slate900.copy(alpha = 0.9f),
                    borderColor = Emerald500.copy(alpha = 0.35f)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChart,
                                    contentDescription = null,
                                    tint = Emerald500,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "PRESUPUESTO GLOBAL MENSUAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )
                                Text(
                                    text = CurrencyUtils.formatCOP(totalSpent) + " / " + CurrencyUtils.formatCOP(totalAllocated),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val totalProgress = if (totalAllocated > 0) (totalSpent / totalAllocated).toFloat().coerceIn(0f, 1f) else 0f
                        LinearProgressIndicator(
                            progress = { totalProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(5.dp)),
                            color = when {
                                totalProgress >= 1f -> RedAlert
                                totalProgress >= 0.8f -> AmberAlert
                                else -> Emerald500
                            },
                            trackColor = Slate700,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (overspentCount > 0) {
                                Surface(
                                    color = RedAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "🚨 $overspentCount SOBREGIRO(S)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RedAlert
                                    )
                                }
                            }

                            if (warningCount > 0) {
                                Surface(
                                    color = AmberAlert.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "⚠️ $warningCount EN ALERTA (>=80%)",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberAlert
                                    )
                                }
                            }

                            if (overspentCount == 0 && warningCount == 0) {
                                Surface(
                                    color = Emerald500.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "✅ TODAS LAS CATEGORÍAS EN RANGO",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Categories List
            items(categories) { category ->
                BudgetCategoryItem(
                    category = category,
                    onEditClick = { editingCategory = category }
                )
            }
        }

        // Edit Budget Dialog
        editingCategory?.let { category ->
            EditCategoryBudgetDialog(
                category = category,
                onDismiss = { editingCategory = null },
                onConfirm = { newLimit ->
                    viewModel.updateCategoryBudgetLimit(category.name, newLimit)
                    editingCategory = null
                }
            )
        }

        // Add New Budget Category Dialog
        if (showNewCategoryDialog) {
            AddCategoryBudgetDialog(
                onDismiss = { showNewCategoryDialog = false },
                onConfirm = { name, limit ->
                    viewModel.updateCategoryBudgetLimit(name, limit)
                    showNewCategoryDialog = false
                }
            )
        }
    }
}

@Composable
fun BudgetCategoryItem(
    category: BudgetCategory,
    onEditClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    val percentage = if (category.allocatedAmount > 0) {
        (category.spentAmount / category.allocatedAmount)
    } else 0.0

    val progress = percentage.toFloat().coerceIn(0f, 1f)

    val (statusText, statusColor) = when {
        percentage >= 1.0 -> "🚨 SOBREGIRO (${(percentage * 100).toInt()}%)" to RedAlert
        percentage >= 0.8 -> "⚠️ ALERTA (${(percentage * 100).toInt()}%)" to AmberAlert
        else -> "✅ OK (${(percentage * 100).toInt()}%)" to Emerald500
    }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = when {
            percentage >= 1.0 -> RedAlert.copy(alpha = 0.5f)
            percentage >= 0.8 -> AmberAlert.copy(alpha = 0.5f)
            else -> GlassBorder
        }
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(statusColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(category.name),
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = category.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = statusText,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onEditClick()
                    },
                    modifier = Modifier.bounceClick(scaleDown = 0.88f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Editar presupuesto",
                        tint = Slate400,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = statusColor,
                trackColor = Slate700,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Gastado: " + CurrencyUtils.formatCOP(category.spentAmount),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percentage >= 1.0) RedAlert else Slate200
                )
                Text(
                    text = "Límite: " + CurrencyUtils.formatCOP(category.allocatedAmount),
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun EditCategoryBudgetDialog(
    category: BudgetCategory,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var limitInput by remember { mutableStateOf(category.allocatedAmount.toInt().toString()) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Presupuesto: ${category.name}",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Establece el límite de gasto mensual para la categoría ${category.name}.",
                    color = Slate400,
                    fontSize = 13.sp
                )

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Límite de Gasto (COP)", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val limit = limitInput.toDoubleOrNull() ?: category.allocatedAmount
                    onConfirm(limit)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.bounceClick(scaleDown = 0.94f)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Slate400)
            }
        }
    )
}

@Composable
fun AddCategoryBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var nameInput by remember { mutableStateOf("") }
    var limitInput by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Nueva Categoría de Presupuesto",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("Nombre de la Categoría", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = limitInput,
                    onValueChange = { limitInput = it },
                    label = { Text("Límite de Gasto (COP)", color = Slate400) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameInput.isNotBlank()) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val limit = limitInput.toDoubleOrNull() ?: 300000.0
                        onConfirm(nameInput.trim(), limit)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.bounceClick(scaleDown = 0.94f)
            ) {
                Text("Crear", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Slate400)
            }
        }
    )
}
