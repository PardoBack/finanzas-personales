package com.example.finanzaspersonales.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.finanzaspersonales.util.NotificationHelper

@Composable
fun AjustesSection(
    isScanning: Boolean,
    onScanGmail: () -> Unit,
    onSendEmailAudit: () -> Unit
) {
    val context = LocalContext.current
    var notifStatusMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título de la sección
        Column {
            Text(
                text = "Ajustes y Personalización",
                color = AppTheme.colors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Tema visual, notificaciones y perfil",
                color = AppTheme.colors.textSecondary,
                fontSize = 12.sp
            )
        }

        // 1. Selector de Apariencia / Tema
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
                            text = "Apariencia Visual",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Elige tu modo favorito. Oscuro por defecto.",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = if (AppTheme.colors.isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                        contentDescription = null,
                        tint = AppTheme.colors.cyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Tarjetas de selección: Oscuro, Claro, Sistema
                val currentMode = ThemeManager.currentTheme
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Oscuro",
                        subtitle = "Por defecto",
                        icon = Icons.Default.DarkMode,
                        isSelected = currentMode == ThemeMode.DARK,
                        onClick = { ThemeManager.setTheme(ThemeMode.DARK) }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Claro",
                        subtitle = "Luminoso",
                        icon = Icons.Default.LightMode,
                        isSelected = currentMode == ThemeMode.LIGHT,
                        onClick = { ThemeManager.setTheme(ThemeMode.LIGHT) }
                    )
                    ThemeOptionCard(
                        modifier = Modifier.weight(1f),
                        title = "Sistema",
                        subtitle = "Auto",
                        icon = Icons.Default.SettingsBrightness,
                        isSelected = currentMode == ThemeMode.SYSTEM,
                        onClick = { ThemeManager.setTheme(ThemeMode.SYSTEM) }
                    )
                }
            }
        }

        // 2. Centro de Notificaciones Funcional
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(AppTheme.colors.emerald.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AppTheme.colors.emeraldBright,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Alertas y Notificaciones",
                                color = AppTheme.colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "Canal activo para vencimientos y pagos",
                                color = AppTheme.colors.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = AppTheme.colors.emerald.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.emerald.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "Activo",
                            color = AppTheme.colors.emeraldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Recibirás avisos oportunos sobre compromisos de Quincena 1 (Día 15) y Fin de Mes (Día 30), así como confirmación de movimientos registrados.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Botón Enviar Notificación de Prueba
                Button(
                    onClick = {
                        NotificationHelper.sendTestNotification(context)
                        notifStatusMessage = "¡Notificación de prueba enviada con éxito! Revisa la barra superior."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.emerald),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Notificación de Prueba",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                notifStatusMessage?.let { msg ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = msg,
                        color = AppTheme.colors.emeraldBright,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Tarjeta de Perfil & Entorno
        Surface(
            color = AppTheme.colors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AppTheme.colors.cyan, AppTheme.colors.violet)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "MP",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Marlon Muñoz Pardo",
                            color = AppTheme.colors.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Administrador Financiero • PardoFinanzas V27",
                            color = AppTheme.colors.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = AppTheme.colors.subtleBorder)
                Spacer(modifier = Modifier.height(14.dp))

                ProfileDetailRow(label = "Cuenta Principal", value = "AyC Colanta (Ahorros)", color = AppTheme.colors.cyan)
                ProfileDetailRow(label = "Nube Google Sheets", value = "Sincronización en vivo", color = AppTheme.colors.emeraldBright)
                ProfileDetailRow(label = "Escáner Gmail", value = "Filtro .ZIP -> FINANZAS", color = AppTheme.colors.amber)
                ProfileDetailRow(label = "Seguridad", value = "Token criptográfico activo", color = AppTheme.colors.violet)
            }
        }

        // 4. Módulo de Facturación Gmail .ZIP
        Surface(
            color = AppTheme.colors.cardBackground,
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "Regla de Facturas Gmail (.ZIP)",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Al procesar correos, el sistema descomprime los adjuntos .ZIP y marca la etiqueta FINANZAS.",
                    color = AppTheme.colors.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onScanGmail,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTheme.colors.cyan),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isScanning) "Escaneando..." else "Escanear Gmail",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    OutlinedButton(
                        onClick = onSendEmailAudit,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Auditoría Email",
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun ThemeOptionCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(95.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) AppTheme.colors.cyan.copy(alpha = 0.18f) else AppTheme.colors.cardElevated,
        border = BorderStroke(
            if (isSelected) 1.5.dp else 1.dp,
            if (isSelected) AppTheme.colors.cyan else AppTheme.colors.subtleBorder
        )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) AppTheme.colors.cyan else AppTheme.colors.textSecondary,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = AppTheme.colors.textMuted,
                fontSize = 9.sp
            )
        }
    }
}

@Composable
private fun ProfileDetailRow(label: String, value: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = AppTheme.colors.textSecondary, fontSize = 12.sp)
        Text(text = value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
