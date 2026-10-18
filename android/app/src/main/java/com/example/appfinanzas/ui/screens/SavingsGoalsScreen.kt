package com.example.appfinanzas.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanzas.data.local.SavingsGoalEntity
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.*
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalsScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.savingsGoalsState.collectAsState()
    var showAddGoalDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                modifier = Modifier.statusBarsPadding(),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Column {
                        Text("Metas y Proyectos", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emerald500)
                        Text("Metas de Ahorro", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
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
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showAddGoalDialog = true
                },
                containerColor = Emerald500,
                contentColor = Color.White,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.bounceClick(scaleDown = 0.92f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nueva Meta")
            }
        }
    ) { padding ->
        if (state.goals.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No tienes metas de ahorro aún.",
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.goals) { goal ->
                    SavingsGoalItem(
                        goal = goal,
                        onContribute = { amount -> viewModel.contributeToGoal(goal.id, amount) }
                    )
                }
            }
        }

        if (showAddGoalDialog) {
            AddGoalDialog(
                onDismiss = { showAddGoalDialog = false },
                onConfirm = { name, target ->
                    viewModel.addSavingsGoal(name, target)
                    showAddGoalDialog = false
                }
            )
        }
    }
}

@Composable
fun SavingsGoalItem(
    goal: SavingsGoalEntity,
    onContribute: (Double) -> Unit
) {
    var showContributeDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        backgroundColor = Slate900.copy(alpha = 0.85f),
        borderColor = GlassBorder
    ) {
        Column {
            Text(goal.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(10.dp))

            val progress = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f) else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Emerald500,
                trackColor = Slate700,
                strokeCap = StrokeCap.Round
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("${(progress * 100).toInt()}% completado", fontSize = 12.sp, color = Emerald500, fontWeight = FontWeight.Bold)
                Text(
                    "${CurrencyUtils.formatCOP(goal.currentAmount)} / ${CurrencyUtils.formatCOP(goal.targetAmount)}",
                    fontSize = 12.sp,
                    color = Slate400,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showContributeDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.End).bounceClick(scaleDown = 0.94f)
            ) {
                Text("Contribuir", fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showContributeDialog) {
        ContributeDialog(
            goalName = goal.name,
            onDismiss = { showContributeDialog = false },
            onConfirm = { amount ->
                onContribute(amount)
                showContributeDialog = false
            }
        )
    }
}

@Composable
fun AddGoalDialog(onDismiss: () -> Unit, onConfirm: (String, Double) -> Unit) {
    var name by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Nueva Meta de Ahorro", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", color = Slate400) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Emerald500,
                        unfocusedBorderColor = Slate700,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                OutlinedTextField(
                    value = target,
                    onValueChange = { target = it },
                    label = { Text("Monto Objetivo", color = Slate400) },
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
                    onConfirm(name, target.toDoubleOrNull() ?: 0.0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.bounceClick(scaleDown = 0.94f)
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Slate400) }
        }
    )
}

@Composable
fun ContributeDialog(goalName: String, onDismiss: () -> Unit, onConfirm: (Double) -> Unit) {
    var amount by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        title = { Text("Contribuir a $goalName", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                label = { Text("Monto", color = Slate400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Emerald500,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onConfirm(amount.toDoubleOrNull() ?: 0.0)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                modifier = Modifier.bounceClick(scaleDown = 0.94f)
            ) { Text("Ahorrar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Slate400) }
        }
    )
}
