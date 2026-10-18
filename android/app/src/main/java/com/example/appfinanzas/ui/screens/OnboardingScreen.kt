package com.example.appfinanzas.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.appfinanzas.ui.components.bounceClick
import com.example.appfinanzas.ui.theme.*

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentStep by remember { mutableIntStateOf(1) }
    val haptic = LocalHapticFeedback.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .systemBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { currentStep / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp),
                color = Emerald500,
                trackColor = Slate800
            )

            AnimatedContent(
                targetState = currentStep,
                label = "step_content",
                modifier = Modifier.weight(1f)
            ) { step ->
                when (step) {
                    1 -> ProfileSetupStep()
                    2 -> SmartImportStep()
                    3 -> GoalDefinitionStep()
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (currentStep < 3) {
                        currentStep++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .bounceClick(scaleDown = 0.94f),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Emerald500,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (currentStep < 3) "Siguiente" else "Comenzar",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
fun ProfileSetupStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.AccountCircle,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Emerald500
        )
        Text(
            "Configura tu Perfil",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            "Queremos conocerte mejor para darte los mejores consejos financieros.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("Nombre Completo", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@Composable
fun SmartImportStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Violet500
        )
        Text(
            "Importación Inteligente",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            "Sincroniza tus extractos de Excel o PDF para automatizar tus movimientos.",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {},
            modifier = Modifier.fillMaxWidth().bounceClick(scaleDown = 0.94f),
            colors = ButtonDefaults.buttonColors(containerColor = Violet500),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Subir Excel/PDF", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun GoalDefinitionStep() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.Flag,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = Emerald500
        )
        Text(
            "Define tus Metas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            "¿Qué quieres lograr? ¿Un viaje, una casa o ahorrar para emergencias?",
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = Slate400
        )
        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = "",
            onValueChange = {},
            label = { Text("¿Cuál es tu objetivo?", color = Slate400) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald500,
                unfocusedBorderColor = Slate700,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
fun OnboardingPreview() {
    FinanzasProTheme {
        OnboardingScreen(onFinish = {})
    }
}
