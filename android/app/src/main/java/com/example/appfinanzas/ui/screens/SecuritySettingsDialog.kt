package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import kotlinx.coroutines.launch

@Composable
fun SecuritySettingsDialog(
    isSecurityEnabled: Boolean,
    isBiometricEnabled: Boolean = false,
    isBiometricSupported: Boolean = true,
    onToggleSecurity: (Boolean) -> Unit,
    onToggleBiometric: (Boolean) -> Unit = {},
    onValidateCurrentPin: suspend (String) -> Boolean,
    onChangePin: suspend (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var isCreatingPinMode by remember { mutableStateOf(false) }
    var isChangingPinMode by remember { mutableStateOf(false) }

    // State for creating new PIN
    var createPinInput by remember { mutableStateOf("") }
    var confirmCreatePinInput by remember { mutableStateOf("") }

    // State for changing PIN
    var currentPinInput by remember { mutableStateOf("") }
    var newPinInput by remember { mutableStateOf("") }
    var confirmPinInput by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Slate900,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Security,
                    contentDescription = null,
                    tint = Emerald500
                )
                Text(
                    text = "Ajustes de Seguridad",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Toggle Security Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Bloqueo con PIN de 4 dígitos",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Solicita PIN al abrir la app o regresar de segundo plano",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = isSecurityEnabled || isCreatingPinMode,
                        onCheckedChange = { checked ->
                            errorMessage = null
                            successMessage = null
                            if (checked) {
                                isCreatingPinMode = true
                                isChangingPinMode = false
                            } else {
                                isCreatingPinMode = false
                                isChangingPinMode = false
                                onToggleSecurity(false)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }

                // PIN Creation Form (when enabling security)
                AnimatedVisibility(visible = isCreatingPinMode && !isSecurityEnabled) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Crear PIN de 4 dígitos",
                            color = Emerald500,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )

                        OutlinedTextField(
                            value = createPinInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) createPinInput = it },
                            label = { Text("Nuevo PIN (4 dígitos)", color = Slate400) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        OutlinedTextField(
                            value = confirmCreatePinInput,
                            onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmCreatePinInput = it },
                            label = { Text("Confirmar PIN", color = Slate400) },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Emerald500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedButton(
                                onClick = {
                                    isCreatingPinMode = false
                                    createPinInput = ""
                                    confirmCreatePinInput = ""
                                    errorMessage = null
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar", color = Slate400)
                            }

                            Button(
                                onClick = {
                                    scope.launch {
                                        errorMessage = null
                                        if (createPinInput.length != 4 || confirmCreatePinInput.length != 4) {
                                            errorMessage = "El PIN debe tener exactamente 4 dígitos."
                                            return@launch
                                        }
                                        if (createPinInput != confirmCreatePinInput) {
                                            errorMessage = "Los PINs ingresados no coinciden."
                                            return@launch
                                        }

                                        onChangePin(createPinInput)
                                        onToggleSecurity(true)
                                        isCreatingPinMode = false
                                        successMessage = "¡PIN configurado y seguridad activada!"
                                        createPinInput = ""
                                        confirmCreatePinInput = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Guardar PIN", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider(color = Slate800, thickness = 1.dp)

                // Biometrics Toggle (Independent Selector)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = if (isSecurityEnabled && isBiometricSupported) Emerald500 else Slate400
                        )
                        Column {
                            Text(
                                text = "Usar Huella / Rostro",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = if (isBiometricSupported) "Desbloqueo biométrico opcional en la pantalla de bloqueo" else "Biometría no disponible en este dispositivo",
                                color = Slate400,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Switch(
                        checked = isBiometricEnabled,
                        onCheckedChange = { onToggleBiometric(it) },
                        enabled = isSecurityEnabled && isBiometricSupported,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Emerald500,
                            uncheckedThumbColor = Slate400,
                            uncheckedTrackColor = Slate800
                        )
                    )
                }

                HorizontalDivider(color = Slate800, thickness = 1.dp)

                // Change PIN Section (if security is already enabled)
                if (isSecurityEnabled && !isCreatingPinMode) {
                    if (!isChangingPinMode) {
                        Button(
                            onClick = {
                                isChangingPinMode = true
                                errorMessage = null
                                successMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Slate800,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleDown = 0.95f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Cambiar PIN de 4 dígitos", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Cambiar PIN de 4 Dígitos",
                                color = Emerald500,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )

                            OutlinedTextField(
                                value = currentPinInput,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) currentPinInput = it },
                                label = { Text("PIN Actual", color = Slate400) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = newPinInput,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) newPinInput = it },
                                label = { Text("Nuevo PIN (4 dígitos)", color = Slate400) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            OutlinedTextField(
                                value = confirmPinInput,
                                onValueChange = { if (it.length <= 4 && it.all { char -> char.isDigit() }) confirmPinInput = it },
                                label = { Text("Confirmar Nuevo PIN", color = Slate400) },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Emerald500,
                                    unfocusedBorderColor = Slate700,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                )
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        isChangingPinMode = false
                                        currentPinInput = ""
                                        newPinInput = ""
                                        confirmPinInput = ""
                                        errorMessage = null
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Cancelar", color = Slate400)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            errorMessage = null
                                            successMessage = null

                                            if (currentPinInput.length != 4 || newPinInput.length != 4 || confirmPinInput.length != 4) {
                                                errorMessage = "El PIN debe tener exactamente 4 dígitos."
                                                return@launch
                                            }

                                            val isValidCurrent = onValidateCurrentPin(currentPinInput)
                                            if (!isValidCurrent) {
                                                errorMessage = "El PIN actual es incorrecto."
                                                return@launch
                                            }

                                            if (newPinInput != confirmPinInput) {
                                                errorMessage = "Los nuevos PINs no coinciden."
                                                return@launch
                                            }

                                            onChangePin(newPinInput)
                                            successMessage = "¡PIN cambiado con éxito!"
                                            isChangingPinMode = false
                                            currentPinInput = ""
                                            newPinInput = ""
                                            confirmPinInput = ""
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Guardar PIN", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                AnimatedVisibility(visible = errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedAlert,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                AnimatedVisibility(visible = successMessage != null) {
                    Text(
                        text = successMessage ?: "",
                        color = Emerald500,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    )
}
