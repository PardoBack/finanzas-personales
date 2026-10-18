package com.example.appfinanzas.ui.screens

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.Emerald500
import com.example.appfinanzas.ui.theme.GlassBorder
import com.example.appfinanzas.ui.theme.RedAlert
import com.example.appfinanzas.ui.theme.Slate400
import com.example.appfinanzas.ui.theme.Slate800
import com.example.appfinanzas.ui.theme.Slate900
import com.example.appfinanzas.ui.theme.Violet500
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SecurityLockScreen(
    onValidatePin: suspend (String) -> Boolean,
    onSuccessUnlock: () -> Unit,
    onResetOrCreatePin: (() -> Unit)? = null,
    isBiometricAvailable: Boolean = true,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scope.launch {
            if (onValidatePin("")) {
                onSuccessUnlock()
            }
        }
    }

    fun triggerError(msg: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        errorMessage = msg
        isError = true
        enteredPin = ""
        scope.launch {
            repeat(4) {
                shakeOffset.animateTo(20f, spring(stiffness = Spring.StiffnessHigh))
                shakeOffset.animateTo(-20f, spring(stiffness = Spring.StiffnessHigh))
            }
            shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
            isError = false
        }
    }

    fun launchBiometricPrompt() {
        val activity = context.findFragmentActivity()
        if (activity != null && isBiometricAvailable) {
            val executor = ContextCompat.getMainExecutor(context)
            val biometricPrompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSuccessUnlock()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON
                        ) {
                            errorMessage = errString.toString()
                        }
                    }
                }
            )

            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Autenticación Biométrica")
                .setSubtitle("Ingresa a Finanzas Pro con tu huella o rostro")
                .setNegativeButtonText("Usar PIN")
                .build()

            try {
                biometricPrompt.authenticate(promptInfo)
            } catch (_: Exception) {
                // Biometric fallback
            }
        }
    }

    // Biometrics is optional and only triggered on demand when user presses "Usar Huella" button

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Slate900,
                        Color(0xFF0D1322),
                        Slate900
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 32.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Emerald500, Violet500)
                            )
                        )
                        .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Security,
                        contentDescription = "Seguridad",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Finanzas Pro",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "Bloqueo de Seguridad Activado",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emerald500
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ingresa tu PIN de 4 dígitos",
                    fontSize = 14.sp,
                    color = Slate400
                )
            }

            // PIN Indicator Dots Section with Shake Animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { index ->
                        val isFilled = index < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isError -> RedAlert
                                        isFilled -> Emerald500
                                        else -> Slate800
                                    }
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = when {
                                        isError -> RedAlert
                                        isFilled -> Emerald500
                                        else -> GlassBorder
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(tween(200)),
                    exit = fadeOut(tween(200))
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = RedAlert,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Numeric Keypad & Optional Biometrics Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                val numRows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                numRows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        row.forEach { digit ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                when (digit) {
                                    "BIO" -> {
                                        if (isBiometricAvailable) {
                                            KeypadIconButton(
                                                icon = Icons.Rounded.Fingerprint,
                                                tint = Emerald500,
                                                onClick = { launchBiometricPrompt() }
                                            )
                                        }
                                    }

                                    "DEL" -> {
                                        KeypadIconButton(
                                            icon = Icons.AutoMirrored.Rounded.Backspace,
                                            tint = Slate400,
                                            onClick = {
                                                if (enteredPin.isNotEmpty()) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    enteredPin = enteredPin.dropLast(1)
                                                    errorMessage = null
                                                }
                                            }
                                        )
                                    }

                                    else -> {
                                        KeypadNumberButton(
                                            number = digit,
                                            onClick = {
                                                if (enteredPin.length < 4) {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    val newPin = enteredPin + digit
                                                    enteredPin = newPin
                                                    errorMessage = null

                                                    if (newPin.length == 4) {
                                                        scope.launch {
                                                            val isValid = onValidatePin(newPin)
                                                            if (isValid) {
                                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                onSuccessUnlock()
                                                            } else {
                                                                triggerError("PIN incorrecto. Intenta de nuevo.")
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Secondary "Usar Huella" Button
                if (isBiometricAvailable) {
                    OutlinedButton(
                        onClick = { launchBiometricPrompt() },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Emerald500.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Emerald500),
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .bounceClick(scaleDown = 0.95f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = Emerald500,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Usar Huella / Rostro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                if (onResetOrCreatePin != null) {
                    TextButton(
                        onClick = { onResetOrCreatePin() },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = "Crear mi primer PIN",
                            color = Emerald500,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadNumberButton(
    number: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .bounceClick(scaleDown = 0.88f),
        shape = CircleShape,
        color = Slate900.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun KeypadIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(64.dp)
            .bounceClick(scaleDown = 0.88f),
        shape = CircleShape,
        color = Slate900.copy(alpha = 0.8f),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

fun Context.findFragmentActivity(): FragmentActivity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is FragmentActivity) {
            return currentContext
        }
        currentContext = currentContext.baseContext
    }
    return null
}
