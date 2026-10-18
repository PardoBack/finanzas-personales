package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.OledBackground
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate600
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.ui.viewmodel.FinanceViewModel
import com.example.appfinanzas.util.CurrencyVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionDialog(
    viewModel: FinanceViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.addTransactionState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var tipo by remember { mutableStateOf(uiState.prefill?.tipo ?: "gasto") }
    var montoStr by remember { mutableStateOf(uiState.prefill?.monto?.toString() ?: "") }
    var concepto by remember { mutableStateOf(uiState.prefill?.concepto ?: "") }
    var categoria by remember { mutableStateOf(uiState.prefill?.categoria ?: "Otros") }
    var cuenta by remember { mutableStateOf("Efectivo") }
    var esPagoFijo by remember { mutableStateOf(uiState.prefill?.esPagoFijo ?: false) }

    // State for fluid spring scale-in expansion
    var isDialogVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isDialogVisible = true
    }

    val categories = listOf("Alimentación", "Transporte", "Servicios", "Ocio", "Hogar", "Salud", "Sueldo", "Otros")
    val accounts = listOf("Nequi", "Bancolombia", "Efectivo")

    // Dynamic accent color morphing based on type (Expense vs Income)
    val accentColor by animateColorAsState(
        targetValue = if (tipo == "gasto") RedAlert else Emerald500,
        animationSpec = tween(400),
        label = "liquidAccentColor"
    )

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            viewModel.resetAddTransactionState()
            onDismiss()
        }
    }

    LaunchedEffect(uiState.prefill) {
        uiState.prefill?.let {
            tipo = it.tipo
            montoStr = it.monto.toString()
            concepto = it.concepto
            categoria = it.categoria
            esPagoFijo = it.esPagoFijo
        }
    }

    Dialog(
        onDismissRequest = {
            viewModel.clearPrefill()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = isDialogVisible,
            enter = scaleIn(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialScale = 0.85f
            ) + fadeIn(animationSpec = tween(300)),
            exit = scaleOut(
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                targetScale = 0.85f
            ) + fadeOut(animationSpec = tween(200))
        ) {
            Scaffold(
                containerColor = OledBackground,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        modifier = Modifier.statusBarsPadding(),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Slate900
                        ),
                        title = {
                            Column {
                                // Morphing Grab Bar for Liquid Modal feel
                                Box(
                                    modifier = Modifier
                                        .size(36.dp, 4.dp)
                                        .clip(CircleShape)
                                        .background(accentColor.copy(alpha = 0.6f))
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (esPagoFijo) "Pagar Gasto Fijo" else "Nueva Transacción",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.clearPrefill()
                                onDismiss()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Slate200)
                            }
                        },
                        actions = {
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    val montoInt = montoStr.toIntOrNull() ?: 0
                                    if (montoInt > 0 && concepto.isNotBlank()) {
                                        viewModel.addTransaction(
                                            concepto = concepto,
                                            monto = montoInt,
                                            tipo = tipo,
                                            categoria = categoria,
                                            cuenta = cuenta,
                                            esPagoFijo = esPagoFijo,
                                            fijoId = uiState.prefill?.fijoId,
                                            periodo = uiState.prefill?.periodo
                                        )
                                    }
                                },
                                enabled = !uiState.isSaving && montoStr.isNotEmpty() && concepto.isNotEmpty(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = accentColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.bounceClick(scaleDown = 0.92f)
                            ) {
                                if (uiState.isSaving) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                                } else {
                                    Text("Guardar", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    )
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .padding(paddingValues)
                        .padding(20.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Selector Tipo Gasto / Ingreso con Morphing
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = tipo == "gasto",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tipo = "gasto"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = RedAlert.copy(alpha = 0.25f),
                                activeContentColor = RedAlert,
                                inactiveContainerColor = Slate800,
                                inactiveContentColor = Slate400
                            )
                        ) {
                            Text("Gasto (-)", fontWeight = FontWeight.Bold)
                        }
                        SegmentedButton(
                            selected = tipo == "ingreso",
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                tipo = "ingreso"
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = Emerald500.copy(alpha = 0.25f),
                                activeContentColor = Emerald500,
                                inactiveContainerColor = Slate800,
                                inactiveContentColor = Slate400
                            )
                        ) {
                            Text("Ingreso (+)", fontWeight = FontWeight.Bold)
                        }
                    }

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        backgroundColor = Slate900.copy(alpha = 0.88f),
                        borderColor = accentColor.copy(alpha = 0.5f)
                    ) {
                        // Monto
                        OutlinedTextField(
                            value = montoStr,
                            onValueChange = { if (it.all { char -> char.isDigit() }) montoStr = it },
                            label = { Text("Monto", color = Slate400) },
                            modifier = Modifier.fillMaxWidth(),
                            prefix = { Text("$ ", color = accentColor, fontWeight = FontWeight.Bold) },
                            visualTransformation = CurrencyVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Concepto
                        OutlinedTextField(
                            value = concepto,
                            onValueChange = { concepto = it },
                            label = { Text("Concepto", color = Slate400) },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Ej: Supermercado o Almuerzo", color = Slate600) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Categoría
                        DropdownField(
                            label = "Categoría",
                            options = categories,
                            selectedOption = categoria,
                            onOptionSelected = { categoria = it },
                            accentColor = accentColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Cuenta
                        DropdownField(
                            label = "Cuenta",
                            options = accounts,
                            selectedOption = cuenta,
                            onOptionSelected = { cuenta = it },
                            accentColor = accentColor
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Pago Fijo Check
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = esPagoFijo,
                                onCheckedChange = { esPagoFijo = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = accentColor,
                                    uncheckedColor = Slate600
                                )
                            )
                            Text("Es un pago fijo recurrente", color = Slate200, fontSize = 14.sp)
                        }
                    }

                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = RedAlert,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    accentColor: Color = Emerald500
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label, color = Slate400) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Slate900)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = Color.White) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
