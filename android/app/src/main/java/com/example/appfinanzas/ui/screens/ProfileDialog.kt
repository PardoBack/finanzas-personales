package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AttachMoney
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.appfinanzas.ui.components.GlassCard
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate200
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate700
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import com.example.appfinanzas.ui.viewmodel.SyncUiState
import com.example.appfinanzas.ui.viewmodel.UserProfileState

@Composable
fun ProfileDialog(
    userProfile: UserProfileState,
    syncState: SyncUiState,
    appsScriptTemplate: String,
    onDismissRequest: () -> Unit,
    onSaveProfile: (name: String, currency: String, salary: Double, webAppUrl: String, geminiKey: String) -> Unit,
    onSyncWithDrive: () -> Unit,
    onOpenSecuritySettings: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    var isAutoDetectActive by remember {
        mutableStateOf(
            Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )?.contains(context.packageName) == true
        )
    }

    var nameInput by remember(userProfile.userName) { mutableStateOf(userProfile.userName) }
    var currencyInput by remember(userProfile.userCurrency) { mutableStateOf(userProfile.userCurrency) }
    var salaryInput by remember(userProfile.averageSalary) { mutableStateOf(userProfile.averageSalary.toLong().toString()) }
    var webAppUrlInput by remember(userProfile.googleWebAppUrl) { mutableStateOf(userProfile.googleWebAppUrl) }
    var geminiKeyInput by remember(userProfile.geminiApiKey) { mutableStateOf(userProfile.geminiApiKey) }

    var isGeminiKeyVisible by remember { mutableStateOf(false) }
    var showAppsScriptTemplate by remember { mutableStateOf(false) }
    var isCopiedToastVisible by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.90f)
                .clip(RoundedCornerShape(32.dp))
                .border(1.dp, GlassBorder, RoundedCornerShape(32.dp)),
            color = Slate900
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Glow
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Emerald500.copy(alpha = 0.12f),
                                    Color.Transparent,
                                    Violet500.copy(alpha = 0.10f)
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = Emerald500.copy(alpha = 0.2f),
                                shape = CircleShape,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Emerald500.copy(alpha = 0.5f))
                            ) {
                                Box(
                                    modifier = Modifier.padding(10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = Emerald500,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Perfil y Configuración Nube",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "Finanzas Pro V25",
                                    fontSize = 12.sp,
                                    color = Slate400
                                )
                            }
                        }

                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismissRequest()
                            },
                            modifier = Modifier.bounceClick()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Cerrar",
                                tint = Slate400
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. User Info Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Slate800.copy(alpha = 0.75f),
                            borderColor = GlassBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "👤 DATOS DEL USUARIO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )

                                OutlinedTextField(
                                    value = nameInput,
                                    onValueChange = { nameInput = it },
                                    label = { Text("Nombre de Usuario") },
                                    leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null, tint = Emerald500) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald500,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Emerald500,
                                        unfocusedLabelColor = Slate400
                                    )
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedTextField(
                                        value = currencyInput,
                                        onValueChange = { currencyInput = it },
                                        label = { Text("Moneda") },
                                        modifier = Modifier.weight(0.4f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Emerald500,
                                            unfocusedBorderColor = Slate700,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Emerald500,
                                            unfocusedLabelColor = Slate400
                                        )
                                    )

                                    OutlinedTextField(
                                        value = salaryInput,
                                        onValueChange = { salaryInput = it.filter { char -> char.isDigit() } },
                                        label = { Text("Ingreso Promedio ($)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        leadingIcon = { Icon(Icons.Rounded.AttachMoney, contentDescription = null, tint = Emerald500) },
                                        modifier = Modifier.weight(0.6f),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Emerald500,
                                            unfocusedBorderColor = Slate700,
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedLabelColor = Emerald500,
                                            unfocusedLabelColor = Slate400
                                        )
                                    )
                                }
                            }
                        }

                        // 2. Google Drive / Sheets Web App Sync Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Slate800.copy(alpha = 0.75f),
                            borderColor = GlassBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📊 GOOGLE DRIVE / SHEETS SYNC",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Violet500
                                    )

                                    TextButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            showAppsScriptTemplate = !showAppsScriptTemplate
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Code,
                                            contentDescription = null,
                                            tint = Violet500,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (showAppsScriptTemplate) "Ocultar Script" else "Ver Script (.gs)",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Violet500
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = webAppUrlInput,
                                    onValueChange = { webAppUrlInput = it },
                                    label = { Text("URL Web App Google Apps Script") },
                                    placeholder = { Text("https://script.google.com/macros/s/.../exec") },
                                    leadingIcon = { Icon(Icons.Rounded.CloudSync, contentDescription = null, tint = Violet500) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Violet500,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Violet500,
                                        unfocusedLabelColor = Slate400
                                    )
                                )

                                // Sync Trigger Button
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onSaveProfile(
                                            nameInput,
                                            currencyInput,
                                            salaryInput.toDoubleOrNull() ?: 2500000.0,
                                            webAppUrlInput,
                                            geminiKeyInput
                                        )
                                        onSyncWithDrive()
                                    },
                                    enabled = !syncState.isSyncing && webAppUrlInput.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(46.dp)
                                        .bounceClick(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Violet500,
                                        contentColor = Color.White
                                    )
                                ) {
                                    if (syncState.isSyncing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sincronizando...")
                                    } else {
                                        Icon(Icons.Rounded.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Sincronizar ahora con Google Drive", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }

                                if (syncState.message != null || syncState.error != null) {
                                    val statusMsg = syncState.message ?: syncState.error ?: ""
                                    val isErr = syncState.error != null
                                    Surface(
                                        color = if (isErr) RedAlert.copy(alpha = 0.15f) else Emerald500.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, if (isErr) RedAlert.copy(alpha = 0.4f) else Emerald500.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = statusMsg,
                                            fontSize = 11.sp,
                                            color = if (isErr) RedAlert else Emerald500,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Auto-detect Card (Nequi, Google Wallet, Bancos)
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isAutoDetectActive) Emerald500.copy(alpha = 0.5f) else Violet500.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Rounded.NotificationsActive,
                                                    contentDescription = null,
                                                    tint = if (isAutoDetectActive) Emerald500 else Violet500,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "Detector Nequi & Billeteras",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.White
                                                )
                                            }
                                            Surface(
                                                color = if (isAutoDetectActive) Emerald500.copy(alpha = 0.2f) else Violet500.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = if (isAutoDetectActive) "ACTIVO" else "PENDIENTE",
                                                    color = if (isAutoDetectActive) Emerald500 else Violet500,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Detecta en vivo tus compras y pagos en Nequi, Google Wallet, Bancolombia, Daviplata y Nu sin digitar nada.",
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            lineHeight = 15.sp
                                        )

                                        Button(
                                            onClick = {
                                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                                context.startActivity(intent)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .bounceClick(),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (isAutoDetectActive) Slate800 else Violet500
                                            ),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(
                                                if (isAutoDetectActive) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = if (isAutoDetectActive) "Permiso Activo (Configuración)" else "Activar Detector Automático",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = showAppsScriptTemplate,
                                    enter = expandVertically(),
                                    exit = shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "📜 Código Script Oficial (.gs)",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Button(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    clipboardManager.setText(AnnotatedString(appsScriptTemplate))
                                                    isCopiedToastVisible = true
                                                },
                                                modifier = Modifier.bounceClick(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                                            ) {
                                                Icon(
                                                    imageVector = if (isCopiedToastVisible) Icons.Rounded.CheckCircle else Icons.Rounded.ContentCopy,
                                                    contentDescription = null,
                                                    tint = if (isCopiedToastVisible) Emerald500 else Color.White,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isCopiedToastVisible) "¡Copiado!" else "Copiar Código",
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        Surface(
                                            color = Slate900,
                                            shape = RoundedCornerShape(12.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                                        ) {
                                            Text(
                                                text = appsScriptTemplate,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                color = Slate200,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .verticalScroll(rememberScrollState())
                                                    .padding(10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Real AI Google Gemini Integration Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Slate800.copy(alpha = 0.75f),
                            borderColor = GlassBorder
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "🤖 IA REAL - GOOGLE GEMINI API",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emerald500
                                )

                                OutlinedTextField(
                                    value = geminiKeyInput,
                                    onValueChange = { geminiKeyInput = it },
                                    label = { Text("Google Gemini API Key") },
                                    placeholder = { Text("AIzaSy...") },
                                    visualTransformation = if (isGeminiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null, tint = Emerald500) },
                                    trailingIcon = {
                                        IconButton(onClick = { isGeminiKeyVisible = !isGeminiKeyVisible }) {
                                            Icon(
                                                imageVector = if (isGeminiKeyVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                                                contentDescription = "Mostrar Key",
                                                tint = Slate400
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Emerald500,
                                        unfocusedBorderColor = Slate700,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedLabelColor = Emerald500,
                                        unfocusedLabelColor = Slate400
                                    )
                                )

                                TextButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        uriHandler.openUri("https://aistudio.google.com/")
                                    },
                                    modifier = Modifier.bounceClick()
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = Emerald500, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Obtener API Key GRATIS en Google AI Studio (1 min)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald500
                                    )
                                }
                            }
                        }

                        // 4. Security Card
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            backgroundColor = Slate800.copy(alpha = 0.75f),
                            borderColor = GlassBorder
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, tint = Emerald500)
                                    Column {
                                        Text("Seguridad & PIN", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                                        Text("Proteger acceso a la aplicación", fontSize = 11.sp, color = Slate400)
                                    }
                                }

                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onOpenSecuritySettings()
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate700)
                                ) {
                                    Text("Configurar", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Save Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveProfile(
                                nameInput,
                                currencyInput,
                                salaryInput.toDoubleOrNull() ?: 2500000.0,
                                webAppUrlInput,
                                geminiKeyInput
                            )
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .bounceClick(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Emerald500,
                            contentColor = Slate900
                        )
                    ) {
                        Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Guardar Cambios de Perfil",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}
