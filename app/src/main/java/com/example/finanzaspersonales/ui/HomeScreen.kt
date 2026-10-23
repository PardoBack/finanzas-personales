package com.example.finanzaspersonales.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.finanzaspersonales.viewmodel.CfoMensaje
import com.example.finanzaspersonales.viewmodel.DashboardUiState
import com.example.finanzaspersonales.viewmodel.GastoFijoItem
import com.example.finanzaspersonales.viewmodel.MainViewModel
import com.example.finanzaspersonales.viewmodel.MovimientoItem
import androidx.compose.ui.platform.LocalContext
import com.example.finanzaspersonales.util.NotificationHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Inicio, 1: Movs, 2: Fijos, 3: Más
    var isBalanceVisible by remember { mutableStateOf(true) }
    var showNewMovementSheet by remember { mutableStateOf(false) }
    var newMovementDefaultType by remember { mutableStateOf("Gasto") }
    var showNewFijoDialog by remember { mutableStateOf(false) }
    var selectedMovimientoDetalle by remember { mutableStateOf<MovimientoItem?>(null) }
    var selectedCategoriaDetalle by remember { mutableStateOf<String?>(null) }
    var subTabMas by remember { mutableIntStateOf(0) } // 0: CFO IA, 1: Colanta & Anual, 2: Gmail .ZIP

    Scaffold(
        containerColor = DeepObsidian,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding(),
        topBar = {
            PardoTopBar(
                uiState = uiState,
                onScanGmail = { viewModel.escanearFacturasGmail() },
                onSyncSheets = { viewModel.sincronizarConSheets(silencioso = false) },
                onOpenCfo = {
                    selectedTab = 3
                    subTabMas = 1
                },
                onToggleTheme = {
                    ThemeManager.toggleDarkLight()
                }
            )
        },
        bottomBar = {
            PardoBottomNav(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                onOpenNewMovement = {
                    newMovementDefaultType = "Gasto"
                    showNewMovementSheet = true
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Banner de estado en vivo
            AnimatedVisibility(
                visible = uiState.statusBanner != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                uiState.statusBanner?.let { bannerText ->
                    Surface(
                        color = CardElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .clickable { viewModel.limpiarBanner() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (uiState.isScanningGmail || uiState.isLoading) {
                                    CircularProgressIndicator(
                                        color = CyanNeon,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = bannerText,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = TextSecondary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .padding(start = 6.dp)
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> InicioTabContent(
                    uiState = uiState,
                    isBalanceVisible = isBalanceVisible,
                    onToggleBalance = { isBalanceVisible = !isBalanceVisible },
                    onSelectMonth = { viewModel.seleccionarMes(it) },
                    onSelectCategory = { selectedCategoriaDetalle = it },
                    onOpenGasto = {
                        newMovementDefaultType = "Gasto"
                        showNewMovementSheet = true
                    },
                    onOpenIngreso = {
                        newMovementDefaultType = "Ingreso"
                        showNewMovementSheet = true
                    },
                    onOpenCfo = {
                        selectedTab = 3
                        subTabMas = 1
                    },
                    onQuickHormiga = { concepto, monto ->
                        viewModel.registrarGastoHormiga(concepto, monto)
                    },
                    onClickMovimiento = { selectedMovimientoDetalle = it },
                    onVerTodosMovs = { selectedTab = 1 },
                    onVerFijos = { selectedTab = 2 },
                    onVerFondos = {
                        selectedTab = 3
                        subTabMas = 2
                    }
                )

                1 -> MovimientosTabContent(
                    uiState = uiState,
                    onClickMovimiento = { selectedMovimientoDetalle = it },
                    onNuevoMovimiento = {
                        newMovementDefaultType = "Gasto"
                        showNewMovementSheet = true
                    }
                )

                2 -> FijosTabContent(
                    uiState = uiState,
                    onPagarFijo = { viewModel.pagarGastoFijo(it) },
                    onRevertirFijo = { viewModel.revertirGastoFijo(it) },
                    onOpenNuevoFijo = { showNewFijoDialog = true }
                )

                3 -> MasTabContent(
                    uiState = uiState,
                    subTab = subTabMas,
                    onSelectSubTab = { subTabMas = it },
                    onSendCfoPrompt = { viewModel.enviarPreguntaCfo(it) },
                    onScanGmail = { viewModel.escanearFacturasGmail() },
                    onSendEmailAudit = { viewModel.enviarAuditoriaEmail() },
                    onSelectMonth = { viewModel.seleccionarMes(it) },
                    onSelectCategoria = { selectedCategoriaDetalle = it }
                )
            }
        }
    }

    if (showNewMovementSheet) {
        NuevoMovimientoModal(
            initialType = newMovementDefaultType,
            onDismiss = { showNewMovementSheet = false },
            onSave = { tipo, desc, cat, valor, cuenta ->
                viewModel.registrarMovimiento(
                    tipo = tipo,
                    descripcion = desc,
                    categoria = cat,
                    valor = valor,
                    cuenta = cuenta
                )
                showNewMovementSheet = false
            }
        )
    }

    if (showNewFijoDialog) {
        NuevoGastoFijoModal(
            onDismiss = { showNewFijoDialog = false },
            onSave = { concepto, categoria, cuota, dia ->
                viewModel.crearGastoFijo(concepto, categoria, cuota, dia)
                showNewFijoDialog = false
            }
        )
    }

    selectedMovimientoDetalle?.let { mov ->
        DetalleMovimientoModal(
            mov = mov,
            onDismiss = { selectedMovimientoDetalle = null },
            onDelete = {
                viewModel.eliminarMovimiento(mov)
                selectedMovimientoDetalle = null
            }
        )
    }

    selectedCategoriaDetalle?.let { cat ->
        DetalleCategoriaModal(
            categoria = cat,
            uiState = uiState,
            onDismiss = { selectedCategoriaDetalle = null },
            onClickMovimiento = { mov ->
                selectedCategoriaDetalle = null
                selectedMovimientoDetalle = mov
            }
        )
    }
}

/* =========================================================
   TOP BAR & BOTTOM NAVIGATION
   ========================================================= */

@Composable
private fun PardoTopBar(
    uiState: DashboardUiState,
    onScanGmail: () -> Unit,
    onSyncSheets: () -> Unit,
    onOpenCfo: () -> Unit,
    onToggleTheme: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Avatar de perfil premium con gradiente y badge pro
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(AppTheme.colors.cyan, AppTheme.colors.emeraldBright)
                        )
                    )
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.cardElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Perfil Marlon",
                    tint = AppTheme.colors.cyan,
                    modifier = Modifier.size(24.dp)
                )
                // Badge PRO / Status en la esquina inferior derecha
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(AppTheme.colors.emeraldBright)
                        .border(1.5.dp, AppTheme.colors.cardBackground, CircleShape)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Marlon",
                    color = AppTheme.colors.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.emeraldBright)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Finanzas • En vivo",
                        color = AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Modo Oscuro / Claro Instantáneo con microinteracción
            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.cardBackground)
                    .border(1.dp, AppTheme.colors.subtleBorder, CircleShape)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = if (AppTheme.colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Cambiar modo visual",
                    tint = if (AppTheme.colors.isDark) AppTheme.colors.amber else AppTheme.colors.cyan,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botón Sincronizar (Appetite UI circular icon plate)
            IconButton(
                onClick = onSyncSheets,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.cardBackground)
                    .border(1.dp, AppTheme.colors.subtleBorder, CircleShape)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sincronizar Sheets",
                    tint = AppTheme.colors.emeraldBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botón Asistente CFO IA (Appetite UI circular icon plate)
            IconButton(
                onClick = onOpenCfo,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.cardBackground)
                    .border(1.dp, AppTheme.colors.subtleBorder, CircleShape)
                    .pressScale(0.92f)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Asistente CFO IA",
                    tint = AppTheme.colors.violet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PardoBottomNav(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenNewMovement: () -> Unit
) {
    Surface(
        color = AppTheme.colors.cardBackground,
        tonalElevation = 10.dp,
        border = BorderStroke(1.dp, AppTheme.colors.subtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                label = "Inicio",
                icon = Icons.Default.Dashboard,
                selected = selectedTab == 0,
                onClick = { onSelectTab(0) }
            )
            BottomNavItem(
                label = "Movs",
                icon = Icons.AutoMirrored.Filled.ReceiptLong,
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) }
            )

            // Botón Central + Nuevo Movimiento con Animación de Entrada y Halo Respiratorio
            CentralFloatingActionButton(onClick = onOpenNewMovement)

            BottomNavItem(
                label = "Fijos",
                icon = Icons.Default.EventRepeat,
                selected = selectedTab == 2,
                onClick = { onSelectTab(2) }
            )
            BottomNavItem(
                label = "Más / IA",
                icon = Icons.Default.Insights,
                selected = selectedTab == 3,
                onClick = { onSelectTab(3) }
            )
        }
    }
}

@Composable
private fun BottomNavItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint = if (selected) AppTheme.colors.cyan else AppTheme.colors.textSecondary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .bounceClick(scaleDown = 0.90f, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

@Composable
private fun CentralFloatingActionButton(
    onClick: () -> Unit
) {
    // 1. Animación de Entrada al abrir la app (Spring Pop-up)
    val entranceScale = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entranceScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 2. Halo respiratorio ambiental infinito (Breathing Glow)
    val infiniteTransition = rememberInfiniteTransition(label = "haloTransition")
    val haloPulse by infiniteTransition.animateFloat(
        initialValue = 52f,
        targetValue = 64f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloSize"
    )
    val haloAlpha by infiniteTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "haloAlpha"
    )

    // 3. Estado de presión táctil con rotación y compresión elástica
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.86f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabPressScale"
    )

    val iconRotation by animateFloatAsState(
        targetValue = if (isPressed) 45f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "fabIconRotation"
    )

    Box(
        modifier = Modifier
            .size(66.dp)
            .graphicsLayer {
                scaleX = entranceScale.value * pressScale
                scaleY = entranceScale.value * pressScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Halo de luz ambiental
        Box(
            modifier = Modifier
                .size(haloPulse.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            AppTheme.colors.cyan.copy(alpha = haloAlpha),
                            Color.Transparent
                        )
                    )
                )
        )

        // Botón Principal
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = CircleShape,
            color = Color.Transparent,
            shadowElevation = 14.dp,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppTheme.colors.cyan, AppTheme.colors.emeraldBright)
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.65f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer {
                            rotationZ = iconRotation
                        }
                ) {
                    val strokeW = 3.6.dp.toPx()
                    val strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    val darkTint = Color(0xFF090D14)
                    val centerOffset = size.width / 2f
                    val armLength = size.width * 0.40f
                    // Barra horizontal con extremos suaves redondeados
                    drawLine(
                        color = darkTint,
                        start = androidx.compose.ui.geometry.Offset(centerOffset - armLength, centerOffset),
                        end = androidx.compose.ui.geometry.Offset(centerOffset + armLength, centerOffset),
                        strokeWidth = strokeW,
                        cap = strokeCap
                    )
                    // Barra vertical con extremos suaves redondeados
                    drawLine(
                        color = darkTint,
                        start = androidx.compose.ui.geometry.Offset(centerOffset, centerOffset - armLength),
                        end = androidx.compose.ui.geometry.Offset(centerOffset, centerOffset + armLength),
                        strokeWidth = strokeW,
                        cap = strokeCap
                    )
                }
            }
        }
    }
}

/* =========================================================
   TAB 0: INICIO (DASHBOARD PRINCIPAL)
   ========================================================= */

@Composable
private fun InicioTabContent(
    uiState: DashboardUiState,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit,
    onSelectMonth: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
    onOpenGasto: () -> Unit,
    onOpenIngreso: () -> Unit,
    onOpenCfo: () -> Unit,
    onQuickHormiga: (String, Long) -> Unit,
    onClickMovimiento: (MovimientoItem) -> Unit,
    onVerTodosMovs: () -> Unit,
    onVerFijos: () -> Unit,
    onVerFondos: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Selector de Períodos y Meses Premium (Appetite UI / Dribbble)
        PremiumMonthSelector(
            uiState = uiState,
            onSelectMonth = onSelectMonth
        )

        // 1. Hero Card de Disponible Real (Appetite UI Dark Mode)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            val heroGradient = if (AppTheme.colors.isDark) {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF131B2A),
                        Color(0xFF0C101A)
                    )
                )
            } else {
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF1F5F9)
                    )
                )
            }
            Column(
                modifier = Modifier
                    .background(heroGradient)
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DISPONIBLE TOTAL",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Privacidad",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(17.dp)
                                .clickable { onToggleBalance() }
                        )
                    }

                    val tasaMes = if (uiState.ingresosMes > 0) {
                        (((uiState.ingresosMes - uiState.gastosMes).toDouble() / uiState.ingresosMes.toDouble()) * 100).toInt().coerceIn(0, 100)
                    } else 14
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "+$tasaMes% este mes",
                            color = EmeraldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isBalanceVisible) "$ ${MainViewModel.formatearNumero(uiState.disponibleReal)}" else "$ •••••••",
                    color = TextPrimary,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3 Mini KPIs de Resumen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Ingresos Mes",
                        value = if (isBalanceVisible) "+$${MainViewModel.formatearNumero(uiState.ingresosMes)}" else "•••••",
                        valueColor = EmeraldBright
                    )
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Gastos Mes",
                        value = if (isBalanceVisible) "-$${MainViewModel.formatearNumero(uiState.gastosMes)}" else "•••••",
                        valueColor = RoseSoft
                    )
                    MiniKpiPill(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onVerFijos() },
                        label = "Fijos Pend.",
                        value = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.fijosPendientes)}" else "•••••",
                        valueColor = AmberGold
                    )
                }
            }
        }

        // 1.1 Tarjeta de Reporte Semanal y Ritmo de Gasto
        val context = LocalContext.current
        val gastosSemana = remember(uiState.movimientos) {
            uiState.movimientos
                .filter { !it.tipo.contains("Ingreso", ignoreCase = true) }
                .take(6)
                .sumOf { it.valor }
        }
        val promedioDiario = if (gastosSemana > 0) gastosSemana / 7 else 0L
        val topCategoriaSemana = remember(uiState.movimientos) {
            uiState.movimientos
                .filter { !it.tipo.contains("Ingreso", ignoreCase = true) }
                .groupBy { it.categoria }
                .maxByOrNull { entry -> entry.value.sumOf { it.valor } }?.key ?: "Alimentación"
        }

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, AppTheme.colors.cyan.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(
                            colors = if (AppTheme.colors.isDark) {
                                listOf(Color(0xFF0F1826), Color(0xFF0A0F19))
                            } else {
                                listOf(Color(0xFFF8FAFC), Color(0xFFF1F5F9))
                            }
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
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
                                .background(AppTheme.colors.cyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = AppTheme.colors.cyan,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "AUDITORÍA SEMANAL",
                                    color = AppTheme.colors.cyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldBright)
                                )
                            }
                            Text(
                                text = "Reporte & Ritmo de Gasto",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Botón para emitir notificación al teléfono
                    Surface(
                        onClick = {
                            NotificationHelper.notifyReporteSemanal(
                                context = context,
                                totalSemana = gastosSemana,
                                topCategoria = topCategoriaSemana,
                                promedioDiario = promedioDiario
                            )
                        },
                        color = AppTheme.colors.cyan.copy(alpha = 0.22f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.cyan.copy(alpha = 0.55f)),
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = AppTheme.colors.cyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Notificar Push",
                                color = AppTheme.colors.cyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                // Bento row con métricas de la semana
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = CardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Gastado Semana",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$ ${MainViewModel.formatearNumero(gastosSemana)}",
                                color = AppTheme.colors.rose,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        color = CardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SubtleBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Ritmo Diario",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$ ${MainViewModel.formatearNumero(promedioDiario)}/d",
                                color = AppTheme.colors.amber,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    Surface(
                        onClick = { onSelectCategory(topCategoriaSemana) },
                        modifier = Modifier
                            .weight(1f)
                            .pressScale(0.95f),
                        color = CardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AppTheme.colors.cyan.copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Mayor Rubro",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Auditar →",
                                    color = AppTheme.colors.cyan,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = topCategoriaSemana,
                                color = AppTheme.colors.emeraldBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Fila de 4 Acciones Rápidas Appetite UI (Gasto, Ingreso, Fijos, CFO IA) con animación elástica
        val isDarkTheme = AppTheme.colors.isDark
        val rosePlate = if (isDarkTheme) Color(0xFF22161B) else Color(0xFFFFEEF1)
        val emeraldPlate = if (isDarkTheme) Color(0xFF13221C) else Color(0xFFE8F8F0)
        val amberPlate = if (isDarkTheme) Color(0xFF251E14) else Color(0xFFFFF7ED)
        val violetPlate = if (isDarkTheme) Color(0xFF201633) else Color(0xFFF5EFFF)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppetiteActionPlate(
                label = "Gasto",
                icon = Icons.Default.Remove,
                iconColor = AppTheme.colors.rose,
                plateColor = rosePlate,
                borderColor = AppTheme.colors.rose.copy(alpha = 0.35f),
                onClick = onOpenGasto
            )
            AppetiteActionPlate(
                label = "Ingreso",
                icon = Icons.Default.Add,
                iconColor = AppTheme.colors.emeraldBright,
                plateColor = emeraldPlate,
                borderColor = AppTheme.colors.emerald.copy(alpha = 0.35f),
                onClick = onOpenIngreso
            )
            AppetiteActionPlate(
                label = "Gastos Fijos",
                icon = Icons.Default.Bolt,
                iconColor = AppTheme.colors.amber,
                plateColor = amberPlate,
                borderColor = AppTheme.colors.amber.copy(alpha = 0.35f),
                onClick = onVerFijos
            )
            AppetiteActionPlate(
                label = "CFO IA",
                icon = Icons.Default.AutoAwesome,
                iconColor = AppTheme.colors.violet,
                plateColor = violetPlate,
                borderColor = AppTheme.colors.violet.copy(alpha = 0.35f),
                onClick = onOpenCfo
            )
        }

        // 2.1 Gastos por Categoría Interactivos con Auditoría Inteligente
        InteractiveCategoriasSection(
            uiState = uiState,
            onSelectCategoria = onSelectCategory
        )

        // 3. Mis Cuentas y Fondos Colanta
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Liquidez por Cuenta & Cooperativa",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ver Fondos →",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onVerFondos() }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountCard(
                    modifier = Modifier.weight(1f),
                    title = "AyC Colanta",
                    subtitle = "Cuenta Nómina",
                    amount = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.aycColanta)}" else "$ •••••",
                    accentColor = EmeraldBright,
                    badge = if (uiState.disponibleReal > 0) "${((uiState.aycColanta.toDouble() / uiState.disponibleReal.toDouble()) * 100).toInt()}% Caja" else "Nómina"
                )
                AccountCard(
                    modifier = Modifier.weight(1f),
                    title = "Efectivo Mano",
                    subtitle = "Billetes y Monedas",
                    amount = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.efectivo)}" else "$ •••••",
                    accentColor = CyanNeon,
                    badge = if (uiState.disponibleReal > 0) "${((uiState.efectivo.toDouble() / uiState.disponibleReal.toDouble()) * 100).toInt()}% Caja" else "Efectivo"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AccountCard(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onVerFondos() },
                    title = "Fondos Colanta",
                    subtitle = "Navideño + Aportes",
                    amount = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.totalFondosColanta)}" else "$ •••••",
                    accentColor = AmberGold,
                    badge = "1 Dic: $2.28M"
                )
                AccountCard(
                    modifier = Modifier.weight(1f),
                    title = "Nequi Digital",
                    subtitle = "Billetera Móvil",
                    amount = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.nequi)}" else "$ •••••",
                    accentColor = VioletAccent,
                    badge = "Sincronizado"
                )
            }
        }

        // 5. Radar de Gastos Hormiga con Botones de 1 Toque "Muy Chimba"
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, SubtleBorder),
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
                            text = "🐜 Registro Rápido Gasto Hormiga",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "1 toque para descontar de Efectivo en Sheets",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "☕",
                        label = "Café",
                        amountText = "$5.000",
                        accentColor = AmberGold,
                        onClick = { onQuickHormiga("Café / Tinto", 5000L) }
                    )
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🍫",
                        label = "Snack / Dulce",
                        amountText = "$8.000",
                        accentColor = RoseSoft,
                        onClick = { onQuickHormiga("Snack / Dulce", 8000L) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🚕",
                        label = "Transporte",
                        amountText = "$15.000",
                        accentColor = CyanNeon,
                        onClick = { onQuickHormiga("Transporte rápido", 15000L) }
                    )
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🥤",
                        label = "Bebida",
                        amountText = "$4.000",
                        accentColor = EmeraldBright,
                        onClick = { onQuickHormiga("Gaseosa / Bebida", 4000L) }
                    )
                }
            }
        }

        // 6. Últimos Movimientos
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Últimos Movimientos",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Ver todos (${uiState.movimientos.size}) →",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onVerTodosMovs() }
                )
            }

            uiState.movimientos.take(6).forEach { mov ->
                MovimientoCard(
                    mov = mov,
                    onClick = { onClickMovimiento(mov) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}


@Composable
private fun AppetiteActionPlate(
    label: String,
    icon: ImageVector,
    iconColor: Color,
    plateColor: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .bounceClick(scaleDown = 0.91f, onClick = onClick)
            .padding(horizontal = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(plateColor)
                .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MiniKpiPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        color = DeepObsidian.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AccountCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    amount: String,
    accentColor: Color,
    badge: String
) {
    Surface(
        modifier = modifier,
        color = CardBackground,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(9.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Surface(
                    color = accentColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(9.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(7.dp))
            Text(
                text = amount,
                color = accentColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HormigaQuickChip(
    modifier: Modifier = Modifier,
    emoji: String,
    label: String,
    amountText: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        color = CardElevated,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emoji, fontSize = 15.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                color = accentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun MovimientoCard(
    mov: MovimientoItem,
    onClick: () -> Unit
) {
    val esIngreso = mov.tipo.contains("Ingreso", ignoreCase = true)
    val amountColor = if (esIngreso) EmeraldBright else RoseSoft
    val signo = if (esIngreso) "+" else "-"
    val emojiCat = iconoPorCategoria(mov.categoria)

    Surface(
        onClick = onClick,
        color = CardBackground,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardElevated),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = emojiCat, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mov.descripcion,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${mov.id} • ${mov.categoria} • ${mov.cuenta}",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$signo$${MainViewModel.formatearNumero(mov.valor)}",
                    color = amountColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = mov.fecha,
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/* =========================================================
   TAB 1: MOVIMIENTOS (HISTORIAL Y FILTROS)
   ========================================================= */

@Composable
private fun MovimientosTabContent(
    uiState: DashboardUiState,
    onClickMovimiento: (MovimientoItem) -> Unit,
    onNuevoMovimiento: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("Todos") }
    val filters = listOf("Todos", "Gastos", "Ingresos", "Colanta", "Efectivo", "Nequi", "Moto", "Alimentación", "Servicios")

    val filteredMovs = remember(uiState.movimientos, searchQuery, activeFilter) {
        uiState.movimientos.filter { m ->
            val matchesQuery = searchQuery.isBlank() ||
                m.descripcion.contains(searchQuery, ignoreCase = true) ||
                m.categoria.contains(searchQuery, ignoreCase = true) ||
                m.id.contains(searchQuery, ignoreCase = true) ||
                m.cuenta.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (activeFilter) {
                "Todos" -> true
                "Gastos" -> !m.tipo.contains("Ingreso", ignoreCase = true)
                "Ingresos" -> m.tipo.contains("Ingreso", ignoreCase = true)
                "Colanta" -> m.cuenta.contains("Colanta", ignoreCase = true) || m.descripcion.contains("Colanta", ignoreCase = true)
                "Efectivo" -> m.cuenta.contains("Efectivo", ignoreCase = true)
                "Nequi" -> m.cuenta.contains("Nequi", ignoreCase = true)
                else -> m.categoria.contains(activeFilter, ignoreCase = true)
            }
            matchesQuery && matchesFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Libro de Movimientos",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Toca cualquier registro para ver detalle o eliminar en Sheets",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Surface(
                color = if (uiState.mesActivo.lowercase(Locale.ROOT) == "octubre") AppTheme.colors.cyan.copy(alpha = 0.16f) else AppTheme.colors.amber.copy(alpha = 0.16f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (uiState.mesActivo.lowercase(Locale.ROOT) == "octubre") AppTheme.colors.cyan.copy(alpha = 0.4f) else AppTheme.colors.amber.copy(alpha = 0.4f))
            ) {
                Text(
                    text = "${uiState.mesActivo.replaceFirstChar { it.uppercase() }} 2026",
                    color = if (uiState.mesActivo.lowercase(Locale.ROOT) == "octubre") AppTheme.colors.cyan else AppTheme.colors.amber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar por comercio, categoría o ID M-xxxx...", fontSize = 13.sp, color = TextSecondary) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = CyanNeon) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CyanNeon,
                unfocusedBorderColor = SubtleBorder,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEach { f ->
                val selected = activeFilter == f
                Surface(
                    onClick = { activeFilter = f },
                    color = if (selected) CyanGlow.copy(alpha = 0.22f) else CardBackground,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (selected) CyanNeon else SubtleBorder)
                ) {
                    Text(
                        text = f,
                        color = if (selected) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredMovs.isEmpty()) {
                Surface(
                    color = CardBackground,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, SubtleBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No se encontraron movimientos con este filtro.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                filteredMovs.forEach { mov ->
                    MovimientoCard(mov = mov, onClick = { onClickMovimiento(mov) })
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/* =========================================================
   TAB 2: GASTOS FIJOS — QUINCENA 1 (DÍA 15) Y FIN DE MES (DÍA 30)
   Sincronizado 1:1 con CONTROL FIJOS en Google Sheets
   ========================================================= */

@Composable
private fun FijosTabContent(
    uiState: DashboardUiState,
    onPagarFijo: (GastoFijoItem) -> Unit,
    onRevertirFijo: (GastoFijoItem) -> Unit,
    onOpenNuevoFijo: () -> Unit
) {
    var filtroSeleccionado by remember { mutableIntStateOf(0) } // 0: Todos, 1: Q1 (Día 15), 2: Q2 (Día 30), 3: Solo Pendientes
    var fijoParaRevertir by remember { mutableStateOf<GastoFijoItem?>(null) }

    val fijosQ1 = remember(uiState.fijos) { uiState.fijos.filter { it.quincena == 1 } }
    val fijosQ2 = remember(uiState.fijos) { uiState.fijos.filter { it.quincena == 2 } }

    val q1TotalCuotas = fijosQ1.sumOf { it.cuota }
    val q1TotalAbonado = fijosQ1.sumOf { it.abonado }
    val q1Pendientes = fijosQ1.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }

    val q2TotalCuotas = fijosQ2.sumOf { it.cuota }
    val q2TotalAbonado = fijosQ2.sumOf { it.abonado }
    val q2Pendientes = fijosQ2.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título del Módulo + Botón "+ Nuevo Fijo"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Control de Gastos Fijos",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Quincena 1 (Día 15) • Quincena 2 (Día 30)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Surface(
                onClick = onOpenNuevoFijo,
                modifier = Modifier.height(34.dp),
                shape = RoundedCornerShape(10.dp),
                color = EmeraldGreen.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Nuevo Fijo", color = EmeraldBright, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 4 KPIs Idénticos a las filas 4-5 de la hoja CONTROL FIJOS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "TOTAL CUOTAS",
                value = "$${MainViewModel.formatearNumero(uiState.totalCuotasFijos)}",
                valueColor = CyanNeon
            )
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "TOTAL ABONADO",
                value = "$${MainViewModel.formatearNumero(uiState.totalAbonadoFijos)}",
                valueColor = EmeraldBright
            )
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "SALDO PENDIENTE",
                value = "$${MainViewModel.formatearNumero(uiState.fijosPendientes)}",
                valueColor = RoseSoft
            )
        }

        // Fila de resumen de Quincena 1 vs Quincena 2
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.weight(1f),
                color = CardBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(CyanNeon))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Q1 (Día 15)", color = CyanNeon, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pendiente: $${MainViewModel.formatearNumero(q1Pendientes)}",
                        color = if (q1Pendientes > 0) AmberGold else EmeraldBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Total: $${MainViewModel.formatearNumero(q1TotalCuotas)}", color = TextSecondary, fontSize = 10.sp)
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                color = CardBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(VioletAccent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Q2 (Día 30)", color = Color(0xFFC4B5FD), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Pendiente: $${MainViewModel.formatearNumero(q2Pendientes)}",
                        color = if (q2Pendientes > 0) AmberGold else EmeraldBright,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text("Total: $${MainViewModel.formatearNumero(q2TotalCuotas)}", color = TextSecondary, fontSize = 10.sp)
                }
            }
        }

        // Filtros Rápidos
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                0 to "Todos (${uiState.fijos.size})",
                1 to "🗓️ Quincena 1 (Día 15)",
                2 to "🗓️ Fin de Mes (Día 30)",
                3 to "⚠️ Solo Pendientes"
            )
            chips.forEach { (id, label) ->
                val active = filtroSeleccionado == id
                Surface(
                    onClick = { filtroSeleccionado = id },
                    color = if (active) CyanGlow.copy(alpha = 0.25f) else CardBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (active) CyanNeon else SubtleBorder)
                ) {
                    Text(
                        text = label,
                        color = if (active) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // =========================================================
        // SECCIÓN 1: QUINCENA 1 — DÍA 15 DE CADA MES
        // =========================================================
        if (filtroSeleccionado == 0 || filtroSeleccionado == 1 || (filtroSeleccionado == 3 && fijosQ1.any { !it.estaPagado })) {
            val itemsQ1 = if (filtroSeleccionado == 3) fijosQ1.filter { !it.estaPagado } else fijosQ1
            QuincenaSectionCard(
                title = "🗓️ Quincena 1 — Día 15 de cada mes",
                tag = "Día 15 • Q1",
                tagColor = CyanNeon,
                subtotalText = "Subtotal: $${MainViewModel.formatearNumero(q1TotalCuotas)} | Abonado: $${MainViewModel.formatearNumero(q1TotalAbonado)}",
                pendienteText = if (q1Pendientes > 0) "Pendiente: $${MainViewModel.formatearNumero(q1Pendientes)} COP" else "¡Quincena 1 100% Pagada!",
                fijosList = itemsQ1,
                onPagarFijo = onPagarFijo,
                onRevertirFijo = { fijoParaRevertir = it }
            )
        }

        // =========================================================
        // SECCIÓN 2: QUINCENA 2 / FIN DE MES — DÍA 30 DE CADA MES
        // =========================================================
        if (filtroSeleccionado == 0 || filtroSeleccionado == 2 || (filtroSeleccionado == 3 && fijosQ2.any { !it.estaPagado })) {
            val itemsQ2 = if (filtroSeleccionado == 3) fijosQ2.filter { !it.estaPagado } else fijosQ2
            QuincenaSectionCard(
                title = "🗓️ Fin de Mes — Día 30 de cada mes",
                tag = "Día 30 • Q2",
                tagColor = Color(0xFFC4B5FD),
                subtotalText = "Subtotal: $${MainViewModel.formatearNumero(q2TotalCuotas)} | Abonado: $${MainViewModel.formatearNumero(q2TotalAbonado)}",
                pendienteText = if (q2Pendientes > 0) "Pendiente: $${MainViewModel.formatearNumero(q2Pendientes)} COP" else "¡Fin de Mes 100% Pagado!",
                fijosList = itemsQ2,
                onPagarFijo = onPagarFijo,
                onRevertirFijo = { fijoParaRevertir = it }
            )
        }

        if (fijoParaRevertir != null) {
            val target = fijoParaRevertir!!
            AlertDialog(
                onDismissRequest = { fijoParaRevertir = null },
                containerColor = CardBackground,
                titleContentColor = TextPrimary,
                textContentColor = TextSecondary,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¿Revertir Pago Fijo?",
                            color = TextPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "¿Estás seguro de que deseas anular el pago de este compromiso?",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                        Surface(
                            color = CardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = target.concepto,
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Monto: $${MainViewModel.formatearNumero(if (target.abonado > 0) target.abonado else target.cuota)} COP",
                                    color = CyanNeon,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                if (target.fechaPago.isNotBlank()) {
                                    Text(
                                        text = "Fecha registrada: ${target.fechaPago}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Text(
                            text = "El gasto volverá a estado 🔴 Pendiente y se eliminará el movimiento de gasto correspondiente para restaurar tu saldo.",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val f = target
                            fijoParaRevertir = null
                            onRevertirFijo(f)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RoseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Confirmar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { fijoParaRevertir = null }
                    ) {
                        Text(
                            text = "Cancelar",
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun QuincenaSectionCard(
    title: String,
    tag: String,
    tagColor: Color,
    subtotalText: String,
    pendienteText: String,
    fijosList: List<GastoFijoItem>,
    onPagarFijo: (GastoFijoItem) -> Unit,
    onRevertirFijo: (GastoFijoItem) -> Unit
) {
    Surface(
        color = CardBackground,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, tagColor.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header de la Quincena
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtotalText,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    color = tagColor.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, tagColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = tag,
                        color = tagColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Surface(
                color = DeepObsidian.copy(alpha = 0.6f),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = pendienteText,
                    color = if (pendienteText.contains("¡")) EmeraldBright else AmberGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            // Lista de Compromisos de la Quincena
            fijosList.forEach { fijo ->
                GastoFijoItemRow(
                    fijo = fijo,
                    onPagar = { onPagarFijo(fijo) },
                    onRevertir = { onRevertirFijo(fijo) }
                )
            }
        }
    }
}

@Composable
private fun GastoFijoItemRow(
    fijo: GastoFijoItem,
    onPagar: () -> Unit,
    onRevertir: () -> Unit
) {
    Surface(
        color = CardElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (fijo.estaPagado) EmeraldGreen.copy(alpha = 0.35f) else SubtleBorder
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = fijo.concepto,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "${fijo.id} • ${fijo.categoria} • ${fijo.medioPago}",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    color = if (fijo.estaPagado) EmeraldGreen.copy(alpha = 0.18f) else RoseRed.copy(alpha = 0.18f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (fijo.estaPagado) EmeraldGreen.copy(alpha = 0.4f) else RoseRed.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = if (fijo.estaPagado) "🟢 Pagado" else "🔴 Pendiente",
                        color = if (fijo.estaPagado) EmeraldBright else RoseSoft,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Cuota: $${MainViewModel.formatearNumero(fijo.cuota)} COP",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (fijo.fechaPago.isNotBlank()) {
                        Text(
                            text = "Pagado el ${fijo.fechaPago}",
                            color = EmeraldBright,
                            fontSize = 11.sp
                        )
                    }
                }

                if (!fijo.estaPagado) {
                    val montoAPagar = if (fijo.saldo > 0) fijo.saldo else fijo.cuota
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (fijo.abonado > 0) {
                            Surface(
                                onClick = onRevertir,
                                modifier = Modifier.height(34.dp),
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF26191B),
                                border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .padding(horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Undo,
                                        contentDescription = "Revertir",
                                        tint = RoseRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Revertir",
                                        color = RoseRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }

                        // Botón de Pago Elegante Fintech
                        Surface(
                            onClick = onPagar,
                            modifier = Modifier.height(34.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF132235),
                            border = BorderStroke(1.dp, CyanNeon.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Pagar $${MainViewModel.formatearNumero(montoAPagar)}",
                                    color = CyanNeon,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = EmeraldBright.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldBright, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Al día", color = EmeraldBright, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Botón Revertir con diálogo de Confirmación
                        Surface(
                            onClick = onRevertir,
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF26191B),
                            border = BorderStroke(1.dp, RoseRed.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Undo,
                                    contentDescription = "Revertir",
                                    tint = RoseRed,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Revertir",
                                    color = RoseRed,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/* =========================================================
   TAB 3: MÁS (CFO IA, FONDOS COLANTA, ANUAL 2026 & REGLA GMAIL)
   ========================================================= */

@Composable
private fun MasTabContent(
    uiState: DashboardUiState,
    subTab: Int,
    onSelectSubTab: (Int) -> Unit,
    onSendCfoPrompt: (String) -> Unit,
    onScanGmail: () -> Unit,
    onSendEmailAudit: () -> Unit,
    onSelectMonth: (String) -> Unit = {},
    onSelectCategoria: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector de módulo secundario (Estadísticas, CFO IA, Fondos & Anual, Ajustes & Tema)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val tabs = listOf(
                0 to "📊 Gráficos",
                1 to "🤖 CFO IA",
                2 to "🏦 Fondos",
                3 to "⚙️ Ajustes"
            )
            tabs.forEach { (idx, title) ->
                val active = subTab == idx
                Surface(
                    onClick = { onSelectSubTab(idx) },
                    modifier = Modifier.weight(1f),
                    color = if (active) AppTheme.colors.cyan.copy(alpha = 0.2f) else AppTheme.colors.cardBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (active) AppTheme.colors.cyan else AppTheme.colors.subtleBorder)
                ) {
                    Text(
                        text = title,
                        color = if (active) AppTheme.colors.cyan else AppTheme.colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        when (subTab) {
            0 -> EstadisticasSection(
                uiState = uiState,
                onSelectCategoria = onSelectCategoria
            )
            1 -> CfoIaSection(
                mensajes = uiState.mensajesCfo,
                onSendPrompt = onSendCfoPrompt
            )
            2 -> ColantaYAnualSection(
                uiState = uiState,
                onSelectMonth = onSelectMonth
            )
            3 -> AjustesSection(
                isScanning = uiState.isScanningGmail,
                onScanGmail = onScanGmail,
                onSendEmailAudit = onSendEmailAudit
            )
        }
    }
}

@Composable
private fun CfoIaSection(
    mensajes: List<CfoMensaje>,
    onSendPrompt: (String) -> Unit
) {
    var inputTexto by remember { mutableStateOf("") }
    val sugerencias = listOf(
        Triple("¿Cuánto tengo disponible?", AppTheme.colors.cyan, Icons.Default.AccountBalanceWallet),
        Triple("¿Qué fijos faltan por pagar?", AppTheme.colors.amber, Icons.Default.EventRepeat),
        Triple("Ver Fondos Colanta", AppTheme.colors.emeraldBright, Icons.Default.Savings),
        Triple("Gasté 25.000 en Almuerzo", AppTheme.colors.rose, Icons.Default.ShoppingCart),
        Triple("Escanear correos .ZIP", AppTheme.colors.violet, Icons.Default.FolderZip)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sugerencias.forEach { (sug, colorAccent, icon) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(colorAccent.copy(alpha = if (AppTheme.colors.isDark) 0.16f else 0.10f))
                        .border(1.dp, colorAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .bounceClick(scaleDown = 0.92f) { onSendPrompt(sug) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = colorAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = sug,
                            color = if (AppTheme.colors.isDark) Color(0xFFF1F5F9) else AppTheme.colors.textPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            mensajes.forEach { msg ->
                val esUser = msg.autor == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (esUser) Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (esUser) CyanGlow.copy(alpha = 0.22f) else CardBackground,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            if (esUser) CyanNeon.copy(alpha = 0.5f) else SubtleBorder
                        ),
                        modifier = Modifier.widthIn(max = 310.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = msg.texto,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                lineHeight = 19.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = msg.hora,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputTexto,
                onValueChange = { inputTexto = it },
                placeholder = { Text("Ej: gasté 15000 en almuerzo...", fontSize = 13.sp, color = TextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyanNeon,
                    unfocusedBorderColor = SubtleBorder,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CyanGlow)
                    .clickable {
                        if (inputTexto.isNotBlank()) {
                            onSendPrompt(inputTexto)
                            inputTexto = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = DeepObsidian)
            }
        }
    }
}

@Composable
private fun ColantaYAnualSection(
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Patrimonio Reservado en Cooperativa Colanta (Appetite Vault Card)
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.2.dp, AmberGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = if (AppTheme.colors.isDark) {
                                listOf(Color(0xFF1E1710), Color(0xFF0F131C))
                            } else {
                                listOf(Color(0xFFFFFBEB), Color(0xFFF8FAFC))
                            }
                        )
                    )
                    .padding(20.dp)
            ) {
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
                                .background(AmberGold.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🏦", fontSize = 18.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "PATRIMONIO & FONDOS COOPERATIVOS",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Cooperativa Colanta 2026",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Surface(
                        color = AmberGold.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "4 Fondos",
                            color = AmberGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "$ ${MainViewModel.formatearNumero(uiState.totalFondosColanta)} COP",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp
                )

                Text(
                    text = "Capital protegido con rentabilidad y ahorro quincenal contractual",
                    color = TextSecondary,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Grilla 2x2 de Fondos Colanta
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FondoBentoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🎄",
                        titulo = "Ahorro Navideño",
                        monto = uiState.ahorroNavideno,
                        tag = "Entrega: 1 Dic",
                        color = AmberGold,
                        progreso = 0.91f
                    )
                    FondoBentoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🤝",
                        titulo = "Aportes Sociales",
                        monto = uiState.aportesCooperativos,
                        tag = "Capital Social",
                        color = CyanNeon,
                        progreso = 0.85f
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FondoBentoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "📈",
                        titulo = "Ahorro Ordinario",
                        monto = uiState.ahorroOrdinario,
                        tag = "Capitalizable",
                        color = EmeraldBright,
                        progreso = 0.70f
                    )
                    FondoBentoCard(
                        modifier = Modifier.weight(1f),
                        emoji = "🛡️",
                        titulo = "Otras Reservas",
                        monto = uiState.otrosAhorros,
                        tag = "Fondo Auxilio",
                        color = VioletAccent,
                        progreso = 0.50f
                    )
                }
            }
        }

        // 2. Trayectoria Anual Consolidada (Enero a Diciembre)
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "DESEMPEÑO ANUAL 2026",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Consolidado fiscal mes a mes (Ene - Dic)",
                            color = TextSecondary,
                            fontSize = 11.5.sp
                        )
                    }
                    Surface(
                        color = EmeraldBright.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${uiState.tasaAhorroAnual}% Ahorro Global",
                            color = EmeraldBright,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // KPI Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Ingresos 2026",
                        value = "$${MainViewModel.formatearNumero(uiState.totalIngresosAnual)}",
                        valueColor = EmeraldBright
                    )
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Gastos 2026",
                        value = "$${MainViewModel.formatearNumero(uiState.totalGastosAnual)}",
                        valueColor = RoseSoft
                    )
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Superávit Neto",
                        value = "$${MainViewModel.formatearNumero(uiState.superavitAnual)}",
                        valueColor = CyanNeon
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Desglose por Mes (Toca para auditar)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Cards de cada mes
                uiState.resumenAnual.forEach { mes ->
                    val esMesActivo = mes.key.equals(uiState.mesActivo, ignoreCase = true)
                    val cardBorder = if (esMesActivo) CyanNeon else SubtleBorder
                    val cardBg = if (esMesActivo) CardElevated else CardElevated.copy(alpha = 0.5f)

                    val statusBadgeText = when (mes.key) {
                        "enero" -> "🏆 Récord Ahorro"
                        "marzo" -> "⭐ Ingreso Extra"
                        "junio" -> "🎁 Mes de Prima"
                        "agosto" -> "🚀 Alto Superávit"
                        "octubre" -> "⚡ Activo en Vivo"
                        else -> "${mes.tasaAhorro}% ahorro"
                    }
                    val statusBadgeColor = when (mes.key) {
                        "enero", "agosto" -> EmeraldBright
                        "marzo", "junio" -> AmberGold
                        "octubre" -> CyanNeon
                        else -> TextSecondary
                    }

                    Surface(
                        onClick = { onSelectMonth(mes.key) },
                        color = cardBg,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .pressScale(0.97f)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (esMesActivo) CyanNeon.copy(alpha = 0.25f)
                                                else CardBackground
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mes.nombre.take(3).uppercase(),
                                            color = if (esMesActivo) CyanNeon else TextSecondary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = mes.nombre,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        color = statusBadgeColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = statusBadgeText,
                                            color = statusBadgeColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "+$ ${MainViewModel.formatearNumero(mes.ahorro)} COP",
                                    color = EmeraldBright,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Métricas Ingresos vs Gastos
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Ingresos: $${MainViewModel.formatearNumero(mes.ingresos)}",
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = "Gastos: $${MainViewModel.formatearNumero(mes.gastos)}",
                                    color = RoseSoft,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Barra de progreso dual / semáforo
                            val ahorroFrac = (mes.tasaAhorro.toFloat() / 100f).coerceIn(0.04f, 1f)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(RoseSoft.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(ahorroFrac)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(
                                            if (mes.tasaAhorro >= 30) EmeraldBright else CyanNeon
                                        )
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

@Composable
private fun FondoBentoCard(
    modifier: Modifier = Modifier,
    emoji: String,
    titulo: String,
    monto: Long,
    tag: String,
    color: Color,
    progreso: Float
) {
    Surface(
        color = CardElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = emoji, fontSize = 16.sp)
                Surface(
                    color = color.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = tag,
                        color = color,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = titulo,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$${MainViewModel.formatearNumero(monto)}",
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black
            )
            Spacer(modifier = Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progreso },
                color = color,
                trackColor = CardBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun FondoRow(label: String, valor: Long, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.weight(1f))
        Text(
            text = "$${MainViewModel.formatearNumero(valor)}",
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun ReglaGmailZipSection(
    isScanning: Boolean,
    onScanGmail: () -> Unit,
    onSendEmailAudit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "📬 MOTOR DE CORREOS GMAIL & DESCOMPRESOR .ZIP",
                    color = CyanNeon,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Flujo Exacto Activo para Marlon Muñoz Pardo:",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "1. Descompresión Universal de .ZIP: Normaliza adjuntos .zip de entidades económicas y facturadores DIAN (Siesa, Colanta, EPM, Tigo, Bancos), extrayendo tanto el XML UBL 2.1 (<AttachedDocument> / <Invoice>) como el PDF interno.\n" +
                        "2. Respeto de No Leído en Bandeja: Cuando llega una factura nueva, el sistema la analiza, la registra en la fila 5 de MOVIMIENTOS, le asigna su etiqueta FINANZAS/<Carpeta>, la deja SIN MARCAR COMO VISTO y la mantiene en tu Bandeja de Entrada.\n" +
                        "3. Movimiento Automático al Leer: Apenas abras el correo o lo marques como leído en Gmail, el sistema lo mueve automáticamente e inmediatamente a su carpeta correspondiente.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(CyanGlow, EmeraldGreen)
                            )
                        )
                        .clickable { onScanGmail() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FolderZip, contentDescription = null, tint = DeepObsidian)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isScanning) "Analizando Bandeja y .ZIP..." else "Ejecutar Analizador Gmail (.ZIP) Ahora",
                            color = DeepObsidian,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }
                }

                OutlinedButton(
                    onClick = onSendEmailAudit,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = EmeraldBright)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enviar Auditoría Ejecutiva 2026 a mi Gmail",
                        color = EmeraldBright,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/* =========================================================
   MODALES: NUEVO MOVIMIENTO, NUEVO FIJO Y DETALLE/ELIMINAR
   ========================================================= */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NuevoMovimientoModal(
    initialType: String,
    onDismiss: () -> Unit,
    onSave: (tipo: String, descripcion: String, categoria: String, valor: Long, cuenta: String) -> Unit
) {
    var tipo by remember { mutableStateOf(initialType) }
    var montoTexto by remember { mutableStateOf("") }
    var descripcion by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf(if (initialType == "Ingreso") "Salario" else "Alimentación") }
    var cuenta by remember { mutableStateOf("AyC Colanta") }

    val categoriasGasto = listOf("Alimentación", "Moto", "Servicios", "Compras", "Transporte", "Vivienda", "Antojos", "Salud", "Entretenimiento")
    val categoriasIngreso = listOf("Salario", "Bonificación", "Otros Ingresos")
    val cuentas = listOf("AyC Colanta", "Efectivo", "Nequi", "Bancolombia")

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = CardBackground,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(GlassBorder)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nuevo Movimiento",
                    color = TextPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Selector Gasto / Ingreso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val esGasto = tipo == "Gasto"
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (esGasto) RoseRed else CardElevated)
                        .border(1.dp, if (esGasto) Color(0xFFFDA4AF) else SubtleBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            tipo = "Gasto"
                            categoria = "Alimentación"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("- Gasto", color = if (esGasto) Color.White else TextSecondary, fontWeight = FontWeight.ExtraBold)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (!esGasto) EmeraldGreen else CardElevated)
                        .border(1.dp, if (!esGasto) EmeraldBright else SubtleBorder, RoundedCornerShape(14.dp))
                        .clickable {
                            tipo = "Ingreso"
                            categoria = "Salario"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ Ingreso", color = if (!esGasto) DeepObsidian else TextSecondary, fontWeight = FontWeight.ExtraBold)
                }
            }

            // Monto Input
            OutlinedTextField(
                value = montoTexto,
                onValueChange = { montoTexto = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monto en COP ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Chips rápidos de valor en pesos COP completos (sin 10K ni abreviaciones)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    5000L to "+$ 5.000",
                    10000L to "+$ 10.000",
                    20000L to "+$ 20.000",
                    50000L to "+$ 50.000"
                ).forEach { (inc, label) ->
                    Surface(
                        onClick = {
                            val actual = montoTexto.toLongOrNull() ?: 0L
                            montoTexto = (actual + inc).toString()
                        },
                        color = CardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = CyanNeon,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 9.dp)
                        )
                    }
                }
            }

            // Concepto
            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Concepto o Comercio (ej: Almuerzo Colanta)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Distribución de Categorías en Cuadrícula de 3 Columnas
            Text(
                text = "Categoría",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            val cats = if (tipo == "Gasto") categoriasGasto else categoriasIngreso
            cats.chunked(3).forEach { rowCats ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowCats.forEach { c ->
                        val sel = categoria == c
                        Surface(
                            onClick = { categoria = c },
                            color = if (sel) {
                                if (tipo == "Gasto") RoseRed.copy(alpha = 0.22f) else EmeraldGreen.copy(alpha = 0.22f)
                            } else CardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (sel) {
                                    if (tipo == "Gasto") RoseRed else EmeraldBright
                                } else SubtleBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 9.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = iconoPorCategoria(c),
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = c,
                                    color = if (sel) {
                                        if (tipo == "Gasto") Color(0xFFFDA4AF) else EmeraldBright
                                    } else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    repeat(3 - rowCats.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Cuenta de Origen / Destino
            Text(
                text = "Cuenta de Origen / Destino",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cuentas.forEach { cta ->
                    val sel = cuenta == cta
                    Surface(
                        onClick = { cuenta = cta },
                        color = if (sel) CyanGlow.copy(alpha = 0.22f) else CardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (sel) CyanNeon else SubtleBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = cta,
                            color = if (sel) CyanNeon else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón Guardar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(CyanGlow, EmeraldGreen)
                        )
                    )
                    .clickable {
                        val valorNum = montoTexto.toLongOrNull() ?: 0L
                        if (valorNum > 0L) {
                            onSave(
                                tipo,
                                descripcion.ifBlank { "$tipo - $categoria" },
                                categoria,
                                valorNum,
                                cuenta
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Guardar en Fila 5 de Google Sheets",
                    color = DeepObsidian,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun NuevoGastoFijoModal(
    onDismiss: () -> Unit,
    onSave: (concepto: String, categoria: String, cuota: Long, dia: Int) -> Unit
) {
    var concepto by remember { mutableStateOf("") }
    var cuotaTexto by remember { mutableStateOf("") }
    var categoria by remember { mutableStateOf("Servicios") }
    var dia by remember { mutableIntStateOf(15) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("Nuevo Compromiso Fijo", color = TextPrimary, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = concepto,
                    onValueChange = { concepto = it },
                    label = { Text("Concepto (ej: Agua, Gimnasio)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = cuotaTexto,
                    onValueChange = { cuotaTexto = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Valor Mensual ($ COP)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { dia = 15 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dia == 15) CyanGlow else CardElevated,
                            contentColor = if (dia == 15) DeepObsidian else TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Quincena 1 (Día 15)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = { dia = 30 },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (dia == 30) CyanGlow else CardElevated,
                            contentColor = if (dia == 30) DeepObsidian else TextSecondary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Fin de Mes (Día 30)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val c = cuotaTexto.toLongOrNull() ?: 0L
                    if (concepto.isNotBlank() && c > 0L) {
                        onSave(concepto, categoria, c, dia)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = DeepObsidian)
            ) {
                Text("Crear Fijo", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun DetalleMovimientoModal(
    mov: MovimientoItem,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Text("${iconoPorCategoria(mov.categoria)} ${mov.descripcion}", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("ID Registro: ${mov.id}", color = CyanNeon, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text("Monto: $${MainViewModel.formatearNumero(mov.valor)} COP", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                Text("Categoría: ${mov.categoria} (${mov.subcategoria})", color = TextSecondary, fontSize = 13.sp)
                Text("Cuenta: ${mov.cuenta} • ${mov.metodo}", color = TextSecondary, fontSize = 13.sp)
                Text("Fecha: ${mov.fecha} ${mov.hora}", color = TextSecondary, fontSize = 12.sp)
            }
        },
        confirmButton = {
            Button(
                onClick = onDelete,
                colors = ButtonDefaults.buttonColors(containerColor = RoseRed, contentColor = Color.White)
            ) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Eliminar de Sheets", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun PremiumMonthSelector(
    uiState: DashboardUiState,
    onSelectMonth: (String) -> Unit
) {
    val meses = listOf(
        "enero" to "Ene", "febrero" to "Feb", "marzo" to "Mar", "abril" to "Abr",
        "mayo" to "May", "junio" to "Jun", "julio" to "Jul", "agosto" to "Ago",
        "septiembre" to "Sep", "octubre" to "Oct", "noviembre" to "Nov", "diciembre" to "Dic"
    )
    val mesActualKey = uiState.mesActivo.lowercase(Locale.ROOT)
    val esMesHistorico = mesActualKey != "octubre"

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardBackground,
        border = BorderStroke(
            1.dp,
            if (esMesHistorico) AmberGold.copy(alpha = 0.45f) else AppTheme.colors.cyan.copy(alpha = 0.25f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (AppTheme.colors.isDark) {
                            listOf(Color(0xFF131B2A), Color(0xFF0C1019))
                        } else {
                            listOf(Color(0xFFFFFFFF), Color(0xFFF1F5F9))
                        }
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Fila Superior: Título con Badge y Botón de Retorno
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (esMesHistorico) AmberGold.copy(alpha = 0.2f)
                                else AppTheme.colors.cyan.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (esMesHistorico) Icons.Default.History else Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = if (esMesHistorico) AmberGold else AppTheme.colors.cyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (esMesHistorico) "AUDITORÍA HISTÓRICA" else "PERÍODO FISCAL 2026",
                            color = if (esMesHistorico) AmberGold else AppTheme.colors.cyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${uiState.mesActivo.replaceFirstChar { it.uppercase() }} 2026",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (esMesHistorico) AmberGold else EmeraldBright)
                            )
                        }
                    }
                }

                if (esMesHistorico) {
                    Surface(
                        onClick = { onSelectMonth("octubre") },
                        color = AmberGold.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.45f)),
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = null,
                                tint = AmberGold,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Volver a Actual ↺",
                                color = AmberGold,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                } else {
                    Surface(
                        color = EmeraldBright.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, EmeraldBright.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldBright)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "En Vivo",
                                color = EmeraldBright,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Barra horizontal de cápsulas de los 12 meses (Aesthetic Dribbble / Appetite)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                meses.forEach { (key, abrev) ->
                    val isSelected = mesActualKey.equals(key, ignoreCase = true)
                    val isOctoberLive = key == "octubre"

                    Surface(
                        onClick = { onSelectMonth(key) },
                        color = if (isSelected) {
                            if (isOctoberLive) AppTheme.colors.cyan.copy(alpha = 0.28f)
                            else AmberGold.copy(alpha = 0.28f)
                        } else CardElevated,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) {
                                if (isOctoberLive) AppTheme.colors.cyan else AmberGold
                            } else if (isOctoberLive) {
                                AppTheme.colors.cyan.copy(alpha = 0.35f)
                            } else {
                                SubtleBorder
                            }
                        ),
                        modifier = Modifier.pressScale(0.92f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isOctoberLive && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.colors.cyan)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = abrev.uppercase(),
                                color = if (isSelected) {
                                    if (isOctoberLive) AppTheme.colors.cyan else AmberGold
                                } else if (isOctoberLive) {
                                    AppTheme.colors.cyan
                                } else {
                                    TextSecondary
                                },
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Banner contextual si se está explorando un mes anterior histórico
            if (esMesHistorico) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberGold.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "📊", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Histórico de ${uiState.mesActivo.replaceFirstChar { it.uppercase() }}: Ingresos +$${MainViewModel.formatearNumero(uiState.ingresosMes)} | Gastos -$${MainViewModel.formatearNumero(uiState.gastosMes)} | Caja $${MainViewModel.formatearNumero(uiState.disponibleReal)} COP",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InteractiveCategoriasSection(
    uiState: DashboardUiState,
    onSelectCategoria: (String) -> Unit
) {
    val gastosMovs = remember(uiState.movimientos, uiState.mesActivo) {
        uiState.movimientos.filter {
            it.tipo.equals("Gasto", ignoreCase = true) &&
            (it.mes.isBlank() || it.mes.equals(uiState.mesActivo, ignoreCase = true))
        }
    }

    val categoriasFrecuentes = listOf(
        "Alimentación" to "🍔",
        "Moto" to "🏍️",
        "Servicios" to "💡",
        "Vivienda" to "🏠",
        "Deudas" to "💳",
        "Compras" to "🛍️"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = CardBackground,
        border = BorderStroke(1.dp, SubtleBorder),
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
                        text = "Gastos por Categoría (${uiState.mesActivo.replaceFirstChar { it.uppercase() }})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Toca para auditar repuestos moto, vivienda y fijos",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
                Surface(
                    color = AppTheme.colors.cyan.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Auditoría IA",
                        color = AppTheme.colors.cyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            val rows = categoriasFrecuentes.chunked(3)
            rows.forEachIndexed { rIdx, rowItems ->
                if (rIdx > 0) Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { (catName, emoji) ->
                        val montoCat = gastosMovs.filter { m ->
                            val cM = m.categoria.lowercase(Locale.ROOT)
                            val cS = catName.lowercase(Locale.ROOT)
                            cM == cS || (cS == "moto" && (cM.contains("moto") || cM.contains("transp") || cM.contains("gasolina"))) ||
                                    (cS == "alimentación" && (cM.contains("alimenta") || cM.contains("comida") || cM.contains("rest"))) ||
                                    (cS == "servicios" && (cM.contains("servicio") || cM.contains("epm"))) ||
                                    (cS == "vivienda" && (cM.contains("vivienda") || cM.contains("arriendo") || cM.contains("hogar"))) ||
                                    (cS == "deudas" && (cM.contains("deuda") || cM.contains("prestamo")))
                        }.sumOf { it.valor }

                        val fijosCatCount = uiState.fijos.count { f ->
                            val cF = f.categoria.lowercase(Locale.ROOT)
                            val cS = catName.lowercase(Locale.ROOT)
                            cF.contains(cS) || cS.contains(cF)
                        }

                        Surface(
                            onClick = { onSelectCategoria(catName) },
                            color = CardElevated,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .weight(1f)
                                .pressScale(0.95f)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = emoji, fontSize = 16.sp)
                                    if (fijosCatCount > 0) {
                                        Surface(
                                            color = AmberGold.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Fijos",
                                                color = AmberGold,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = catName,
                                    color = TextPrimary,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (montoCat > 0) "$${MainViewModel.formatearNumero(montoCat)}" else if (fijosCatCount > 0) "En fijos" else "$0",
                                    color = if (montoCat > 0) RoseSoft else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetalleCategoriaModal(
    categoria: String,
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    onClickMovimiento: (MovimientoItem) -> Unit = {}
) {
    val catKey = categoria.lowercase(Locale.ROOT)

    val movsCat = remember(uiState.movimientos, categoria, uiState.mesActivo) {
        uiState.movimientos.filter { m ->
            val coincideMes = m.mes.isBlank() || m.mes.equals(uiState.mesActivo, ignoreCase = true)
            if (!coincideMes) return@filter false
            val cM = m.categoria.lowercase(Locale.ROOT)
            cM == catKey ||
                    (catKey == "moto" && (cM.contains("moto") || cM.contains("transp") || cM.contains("gasolina"))) ||
                    (catKey == "alimentación" && (cM.contains("alimenta") || cM.contains("comida") || cM.contains("rest"))) ||
                    (catKey == "servicios" && (cM.contains("servicio") || cM.contains("epm") || cM.contains("agua") || cM.contains("luz"))) ||
                    (catKey == "vivienda" && (cM.contains("vivienda") || cM.contains("arriendo") || cM.contains("hogar") || cM.contains("casa"))) ||
                    (catKey == "deudas" && (cM.contains("deuda") || cM.contains("prestamo") || cM.contains("credito") || cM.contains("banco"))) ||
                    (catKey == "compras" && (cM.contains("compra") || cM.contains("ropa") || cM.contains("moda")))
        }
    }

    val fijosCat = remember(uiState.fijos, categoria) {
        uiState.fijos.filter { f ->
            val cF = f.categoria.lowercase(Locale.ROOT)
            val conc = f.concepto.lowercase(Locale.ROOT)
            cF.contains(catKey) || catKey.contains(cF) ||
                    (catKey == "moto" && (cF.contains("moto") || conc.contains("moto") || conc.contains("gasolina") || conc.contains("parqueadero"))) ||
                    (catKey == "vivienda" && (cF.contains("vivienda") || conc.contains("arriendo") || conc.contains("hogar"))) ||
                    (catKey == "servicios" && (cF.contains("servicio") || conc.contains("epm") || conc.contains("energia") || conc.contains("internet"))) ||
                    (catKey == "deudas" && (cF.contains("deuda") || conc.contains("prestamo") || conc.contains("credito")))
        }
    }

    val totalGastosCat = remember(movsCat) {
        movsCat.filter { it.tipo.equals("Gasto", ignoreCase = true) }.sumOf { it.valor }
    }
    val totalGastosMes = uiState.gastosMes.coerceAtLeast(1L)
    val pctMes = ((totalGastosCat.toDouble() / totalGastosMes.toDouble()) * 100).toInt()

    val mayorGastoItem = remember(movsCat) {
        movsCat.filter { it.tipo.equals("Gasto", ignoreCase = true) }.maxByOrNull { it.valor }
    }
    val promedioGasto = if (movsCat.isNotEmpty()) totalGastosCat / movsCat.size.coerceAtLeast(1) else 0L

    val diagnosticoNarrativo = remember(categoria, totalGastosCat, mayorGastoItem, fijosCat) {
        when {
            catKey.contains("moto") || catKey.contains("transp") -> {
                val mayorTexto = if (mayorGastoItem != null) "Tu mayor egreso fue **${mayorGastoItem.descripcion}** por **$${MainViewModel.formatearNumero(mayorGastoItem.valor)} COP** (${mayorGastoItem.cuenta})." else "Sin desembolsos imprevistos registrados."
                "🏍️ **Auditoría de Movilidad & Moto:**\nHas destinado recursos a piezas de reemplazo mecánico, mantenimiento preventivo (lavadas), combustible y parqueaderos de trabajo. $mayorTexto\n• **Recomendación Pro:** Mantener el seguimiento de repuestos y afinación protege la vida útil de la moto sin generar picos de gasto inesperados en tu disponible."
            }
            catKey.contains("vivienda") || catKey.contains("arriendo") || catKey.contains("hogar") -> {
                val totalFijosVivienda = fijosCat.sumOf { it.cuota }
                "🏠 **Auditoría de Vivienda & Hogar:**\nConsolidado de canon de arrendamiento y adquisición de partes o mejoras locativas para la vivienda. Representa tu compromiso esencial de habitabilidad con una cuota mensual programada de **$${MainViewModel.formatearNumero(totalFijosVivienda)} COP** en gastos fijos.\n• **Estado:** Mantener este rubro al día garantiza tu estabilidad y bienestar sin comprometer fondos de ahorro."
            }
            catKey.contains("alimenta") || catKey.contains("comida") -> {
                val mayorTexto = if (mayorGastoItem != null) "Mayor consumo: **${mayorGastoItem.descripcion}** ($${MainViewModel.formatearNumero(mayorGastoItem.valor)} COP)." else ""
                "🍔 **Auditoría de Nutrición & Alimentación:**\nConsumos distribuidos entre la provisión principal de mercado quincenal y almuerzos corporativos en la planta Medellín de Colanta. $mayorTexto\n• **Optimización:** Almorzar en el casino de Colanta (~$11.500 COP) te permite un ahorro superior al 45% respecto a restaurantes externos, manteniendo tu costo por comida en niveles altamente eficientes."
            }
            catKey.contains("servicio") || catKey.contains("epm") -> {
                "💡 **Auditoría de Servicios Públicos:**\nCobertura de energía eléctrica con EPM, agua, gas e internet residencial. Todos los servicios esenciales están al día y sin cobros por mora o reconexión.\n• **Control:** Tu consumo se mantiene estable en el rango presupuestado del mes."
            }
            catKey.contains("deuda") || catKey.contains("prestamo") || catKey.contains("credito") -> {
                val totalCuotas = fijosCat.sumOf { it.cuota }
                "💳 **Auditoría de Desendeudamiento & Pasivos:**\nAbonos a obligaciones bancarias y préstamos personales. Tu compromiso programado mensual es de **$${MainViewModel.formatearNumero(totalCuotas)} COP**.\n• **Impacto Positivo:** Cada cuota cancelada reduce el pasivo total y libera flujo de caja libre para potenciar tus fondos en Cooperativa Colanta."
            }
            else -> {
                val mayorTexto = if (mayorGastoItem != null) "Mayor compra: **${mayorGastoItem.descripcion}** ($${MainViewModel.formatearNumero(mayorGastoItem.valor)} COP)." else ""
                "📊 **Auditoría Financiera de $categoria:**\nRegistras ${movsCat.size} movimientos en ${uiState.mesActivo} con un desembolso total de **$${MainViewModel.formatearNumero(totalGastosCat)} COP** (promedio $${MainViewModel.formatearNumero(promedioGasto)} COP por movimiento). $mayorTexto Representa el $pctMes% de tus egresos mensuales."
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextSecondary.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header del Modal con Icono Gigante y Nombre
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(AppTheme.colors.cyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = iconoPorCategoria(categoria), fontSize = 24.sp)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = categoria.uppercase(),
                            color = AppTheme.colors.cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Auditoría & Desglose",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                Surface(
                    onClick = onDismiss,
                    shape = CircleShape,
                    color = CardElevated,
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Card Principal de KPIs de la Categoría
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = CardElevated,
                border = BorderStroke(1.dp, SubtleBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TOTAL GASTADO EN ${uiState.mesActivo.uppercase()}",
                        color = TextSecondary,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$ ${MainViewModel.formatearNumero(totalGastosCat)} COP",
                        color = TextPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MiniKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "% del Gasto Mes",
                            value = "$pctMes%",
                            valueColor = if (pctMes >= 30) RoseSoft else AppTheme.colors.cyan
                        )
                        MiniKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Movimientos",
                            value = "${movsCat.size} regs",
                            valueColor = TextPrimary
                        )
                        MiniKpiPill(
                            modifier = Modifier.weight(1f),
                            label = "Fijos Asociados",
                            value = "${fijosCat.size} items",
                            valueColor = AmberGold
                        )
                    }
                }
            }

            // Diagnóstico Financiero Inteligente (Smart CFO)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (AppTheme.colors.isDark) Color(0xFF161B2A) else Color(0xFFF1F5F9),
                border = BorderStroke(1.2.dp, AppTheme.colors.cyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = AppTheme.colors.cyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DIAGNÓSTICO & ANÁLISIS DETALLADO",
                            color = AppTheme.colors.cyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = diagnosticoNarrativo,
                        color = TextPrimary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Mayor Gasto Destacado
            if (mayorGastoItem != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardElevated,
                    border = BorderStroke(1.dp, RoseSoft.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoseSoft.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "👑", fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MAYOR GASTO REGISTRADO",
                                color = RoseSoft,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = mayorGastoItem.descripcion,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${mayorGastoItem.fecha} • Cuenta: ${mayorGastoItem.cuenta}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                        Text(
                            text = "-$ ${MainViewModel.formatearNumero(mayorGastoItem.valor)}",
                            color = RoseSoft,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Compromisos Fijos de esta Categoría
            if (fijosCat.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Compromisos Fijos de $categoria",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${fijosCat.size} programados",
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    fijosCat.forEach { fijo ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = CardElevated,
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = fijo.concepto,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Vence día ${fijo.dia} • Q${fijo.quincena} • ${fijo.medioPago}",
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$ ${MainViewModel.formatearNumero(fijo.cuota)} COP",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Surface(
                                        color = if (fijo.estaPagado) EmeraldBright.copy(alpha = 0.15f) else AmberGold.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = if (fijo.estaPagado) "PAGADO" else "PENDIENTE",
                                            color = if (fijo.estaPagado) EmeraldBright else AmberGold,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Historial Detallado de Transacciones en esta Categoría
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Movimientos Registrados (${movsCat.size})",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                if (movsCat.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = CardElevated,
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Sin egresos manuales en $categoria este mes",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    movsCat.forEach { mov ->
                        Surface(
                            onClick = { onClickMovimiento(mov) },
                            shape = RoundedCornerShape(14.dp),
                            color = CardElevated,
                            border = BorderStroke(1.dp, SubtleBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .pressScale(0.97f)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (mov.tipo.equals("Gasto", ignoreCase = true)) RoseSoft.copy(alpha = 0.15f)
                                                else EmeraldBright.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(text = iconoPorCategoria(mov.categoria), fontSize = 15.sp)
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = mov.descripcion,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${mov.fecha} • ${mov.cuenta} (${mov.metodo})",
                                            color = TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                Text(
                                    text = "${if (mov.tipo.equals("Gasto", ignoreCase = true)) "-" else "+"}$ ${MainViewModel.formatearNumero(mov.valor)}",
                                    color = if (mov.tipo.equals("Gasto", ignoreCase = true)) RoseSoft else EmeraldBright,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

fun iconoPorCategoria(cat: String): String {
    val c = cat.lowercase(Locale.ROOT)
    return when {
        c.contains("alimenta") || c.contains("comida") || c.contains("rest") -> "🍔"
        c.contains("moto") || c.contains("gasolina") -> "🏍️"
        c.contains("servicio") || c.contains("epm") || c.contains("agua") -> "💡"
        c.contains("vivienda") || c.contains("arriendo") || c.contains("hogar") -> "🏠"
        c.contains("deuda") || c.contains("prestamo") || c.contains("credito") -> "💳"
        c.contains("compra") || c.contains("ropa") -> "🛍️"
        c.contains("transporte") || c.contains("taxi") || c.contains("parqueadero") -> "🚗"
        c.contains("antojo") || c.contains("hormiga") || c.contains("dulce") -> "🐜"
        c.contains("salud") || c.contains("med") -> "💊"
        c.contains("entretenimiento") || c.contains("suscrip") || c.contains("spotify") -> "🎮"
        c.contains("salario") || c.contains("nomina") || c.contains("nómina") -> "💰"
        c.contains("colanta") || c.contains("fondo") || c.contains("ahorro") -> "🏦"
        else -> "🏷️"
    }
}
