package com.example.finanzaspersonales.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
                    subTabMas = 0
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
                        border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.45f)),
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
                    onOpenGasto = {
                        newMovementDefaultType = "Gasto"
                        showNewMovementSheet = true
                    },
                    onOpenIngreso = {
                        newMovementDefaultType = "Ingreso"
                        showNewMovementSheet = true
                    },
                    onScanGmail = { viewModel.escanearFacturasGmail() },
                    onQuickHormiga = { concepto, monto ->
                        viewModel.registrarGastoHormiga(concepto, monto)
                    },
                    onClickMovimiento = { selectedMovimientoDetalle = it },
                    onVerTodosMovs = { selectedTab = 1 },
                    onVerFijos = { selectedTab = 2 },
                    onVerFondos = {
                        selectedTab = 3
                        subTabMas = 1
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
                    onOpenNuevoFijo = { showNewFijoDialog = true }
                )

                3 -> MasTabContent(
                    uiState = uiState,
                    subTab = subTabMas,
                    onSelectSubTab = { subTabMas = it },
                    onSendCfoPrompt = { viewModel.enviarPreguntaCfo(it) },
                    onScanGmail = { viewModel.escanearFacturasGmail() },
                    onSendEmailAudit = { viewModel.enviarAuditoriaEmail() }
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
}

/* =========================================================
   TOP BAR & BOTTOM NAVIGATION
   ========================================================= */

@Composable
private fun PardoTopBar(
    uiState: DashboardUiState,
    onScanGmail: () -> Unit,
    onSyncSheets: () -> Unit,
    onOpenCfo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyanGlow, VioletAccent)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MP",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Marlon Muñoz Pardo",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(EmeraldBright)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PardoFinanzas V26.9 • En vivo",
                        color = TextSecondary,
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
            // Botón Escanear Gmail (.ZIP)
            Surface(
                onClick = onScanGmail,
                color = CardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.4f)),
                modifier = Modifier.height(38.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailUnread,
                        contentDescription = "Escanear Gmail .ZIP",
                        tint = CyanNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = ".ZIP",
                        color = CyanNeon,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }

            // Botón Sincronizar
            IconButton(
                onClick = onSyncSheets,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardBackground)
                    .border(1.dp, SubtleBorder, RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Sync,
                    contentDescription = "Sincronizar Sheets",
                    tint = EmeraldBright,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Botón CFO IA
            IconButton(
                onClick = onOpenCfo,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(VioletAccent.copy(alpha = 0.18f))
                    .border(1.dp, VioletAccent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "CFO IA",
                    tint = Color(0xFFC4B5FD),
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
        color = Color(0xFF0B1120),
        tonalElevation = 8.dp,
        border = BorderStroke(1.dp, SubtleBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
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
                icon = Icons.Default.ReceiptLong,
                selected = selectedTab == 1,
                onClick = { onSelectTab(1) }
            )

            // Botón Central + Nuevo Movimiento
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CyanGlow, EmeraldGreen)
                        )
                    )
                    .clickable { onOpenNewMovement() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nuevo Movimiento",
                    tint = DeepObsidian,
                    modifier = Modifier.size(28.dp)
                )
            }

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
    val tint = if (selected) CyanNeon else TextSecondary
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp),
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

/* =========================================================
   TAB 0: INICIO (DASHBOARD PRINCIPAL)
   ========================================================= */

@Composable
private fun InicioTabContent(
    uiState: DashboardUiState,
    isBalanceVisible: Boolean,
    onToggleBalance: () -> Unit,
    onSelectMonth: (String) -> Unit,
    onOpenGasto: () -> Unit,
    onOpenIngreso: () -> Unit,
    onScanGmail: () -> Unit,
    onQuickHormiga: (String, Long) -> Unit,
    onClickMovimiento: (MovimientoItem) -> Unit,
    onVerTodosMovs: () -> Unit,
    onVerFijos: () -> Unit,
    onVerFondos: () -> Unit
) {
    val meses = listOf(
        "enero" to "Ene", "febrero" to "Feb", "marzo" to "Mar", "abril" to "Abr",
        "mayo" to "May", "junio" to "Jun", "julio" to "Jul", "agosto" to "Ago",
        "septiembre" to "Sep", "octubre" to "Oct", "noviembre" to "Nov", "diciembre" to "Dic"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Selector de Meses Horizontal
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            meses.forEach { (key, shortLabel) ->
                val active = uiState.mesActivo.equals(key, ignoreCase = true)
                Surface(
                    onClick = { onSelectMonth(key) },
                    color = if (active) CyanGlow.copy(alpha = 0.22f) else CardBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (active) CyanNeon else SubtleBorder
                    )
                ) {
                    Text(
                        text = "$shortLabel 26",
                        color = if (active) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp)
                    )
                }
            }
        }

        // 2. Hero Card de Disponible Real
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF13203D),
                                Color(0xFF0C1324)
                            )
                        )
                    )
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "DISPONIBLE REAL (${uiState.mesActivo.uppercase()})",
                            color = CyanNeon,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Privacidad",
                            tint = TextSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { onToggleBalance() }
                        )
                    }

                    val tasaMes = if (uiState.ingresosMes > 0) {
                        (((uiState.ingresosMes - uiState.gastosMes).toDouble() / uiState.ingresosMes.toDouble()) * 100).toInt().coerceIn(0, 100)
                    } else 30
                    Surface(
                        color = EmeraldGreen.copy(alpha = 0.18f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "$tasaMes% Libre",
                            color = EmeraldBright,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = if (isBalanceVisible) "$ ${MainViewModel.formatearNumero(uiState.disponibleReal)}" else "$ •••••••",
                    color = TextPrimary,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.8).sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 3 Mini KPIs del Mes (Ingresos, Gastos, Fijos Pendientes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Ingresos Mes",
                        value = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.ingresosMes)}" else "••••",
                        valueColor = EmeraldBright
                    )
                    MiniKpiPill(
                        modifier = Modifier.weight(1f),
                        label = "Gastos Mes",
                        value = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.gastosMes)}" else "••••",
                        valueColor = RoseSoft
                    )
                    MiniKpiPill(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onVerFijos() },
                        label = "Fijos Pend.",
                        value = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.fijosPendientes)}" else "••••",
                        valueColor = AmberGold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Botones principales bien diseñados (48dp+)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenGasto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RoseRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Registrar Gasto", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }

                    Button(
                        onClick = onOpenIngreso,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldGreen,
                            contentColor = DeepObsidian
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ingreso", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                    }
                }
            }
        }

        // 3. Tarjeta de Escáner Gmail (.ZIP) + Regla Automática No Leído -> Carpeta
        Surface(
            onClick = onScanGmail,
            shape = RoundedCornerShape(18.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, CyanGlow.copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CyanGlow.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderZip,
                            contentDescription = "Analizador ZIP",
                            tint = CyanNeon,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Analizador Facturas Gmail & .ZIP",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Analiza .ZIP/XML • Deja sin leer en bandeja • Al leer se mueve a su carpeta",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = CyanGlow.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = if (uiState.isScanningGmail) "..." else "Escanear",
                        color = CyanNeon,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // 4. Mis Cuentas y Fondos Colanta (Grid 2x2 perfectamente alineado)
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
                    badge = "94% Caja"
                )
                AccountCard(
                    modifier = Modifier.weight(1f),
                    title = "Efectivo Mano",
                    subtitle = "Billetes y Monedas",
                    amount = if (isBalanceVisible) "$${MainViewModel.formatearNumero(uiState.efectivo)}" else "$ •••••",
                    accentColor = CyanNeon,
                    badge = "Disponible"
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

        // 5. Radar de Gastos Hormiga (Botones de 1 Toque)
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = CardBackground,
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "🐜 Registro Rápido Gasto Hormiga",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Toca un botón para descontar en 1 clic de Efectivo",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "☕",
                        label = "Café",
                        amountText = "$5.000",
                        onClick = { onQuickHormiga("Café / Tinto", 5000L) }
                    )
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🍫",
                        label = "Snack / Dulce",
                        amountText = "$8.000",
                        onClick = { onQuickHormiga("Snack / Dulce", 8000L) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🚕",
                        label = "Transporte",
                        amountText = "$15.000",
                        onClick = { onQuickHormiga("Transporte rápido", 15000L) }
                    )
                    HormigaQuickChip(
                        modifier = Modifier.weight(1f),
                        emoji = "🥤",
                        label = "Bebida",
                        amountText = "$4.000",
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
private fun MiniKpiPill(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        color = DeepObsidian.copy(alpha = 0.65f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = TextSecondary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
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
        shape = RoundedCornerShape(16.dp),
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
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(accentColor)
                )
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
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
            Spacer(modifier = Modifier.height(6.dp))
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
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(46.dp),
        color = CardElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SubtleBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = amountText,
                color = AmberGold,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${mov.id} • ${mov.categoria} • ${mov.cuenta}",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            Button(
                onClick = onNuevoMovimiento,
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepObsidian),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("+ Nuevo", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
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
   TAB 2: COMPROMISOS FIJOS (QUINCENA 1 Y QUINCENA 2)
   ========================================================= */

@Composable
private fun FijosTabContent(
    uiState: DashboardUiState,
    onPagarFijo: (GastoFijoItem) -> Unit,
    onOpenNuevoFijo: () -> Unit
) {
    var filtroQuincena by remember { mutableIntStateOf(0) } // 0: Todos, 1: Q1 (Día 1-15), 2: Q2 (Día 16-30), 3: Pendientes

    val fijosFiltrados = remember(uiState.fijos, filtroQuincena) {
        when (filtroQuincena) {
            1 -> uiState.fijos.filter { it.quincena == 1 }
            2 -> uiState.fijos.filter { it.quincena == 2 }
            3 -> uiState.fijos.filter { !it.estaPagado }
            else -> uiState.fijos
        }
    }

    val totalCuotas = uiState.fijos.sumOf { it.cuota }
    val totalPagado = uiState.fijos.sumOf { it.abonado }
    val totalPendiente = uiState.fijos.filter { !it.estaPagado }.sumOf { (it.cuota - it.abonado).coerceAtLeast(0L) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
                    text = "Organizados por Quincena 1 (Día 15) y Quincena 2 (Día 30)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            Button(
                onClick = onOpenNuevoFijo,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = DeepObsidian),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text("+ Fijo", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        // Resumen de Fijos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "Total Fijos Mes",
                value = "$${MainViewModel.formatearNumero(totalCuotas)}",
                valueColor = CyanNeon
            )
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "Ya Pagado",
                value = "$${MainViewModel.formatearNumero(totalPagado)}",
                valueColor = EmeraldBright
            )
            MiniKpiPill(
                modifier = Modifier.weight(1f),
                label = "Por Pagar",
                value = "$${MainViewModel.formatearNumero(totalPendiente)}",
                valueColor = AmberGold
            )
        }

        // Filtros de Quincena
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val chips = listOf(
                0 to "Todos (${uiState.fijos.size})",
                1 to "Quincena 1 (Día 15)",
                2 to "Quincena 2 (Día 30)",
                3 to "Pendientes"
            )
            chips.forEach { (id, label) ->
                val active = filtroQuincena == id
                Surface(
                    onClick = { filtroQuincena = id },
                    color = if (active) CyanGlow.copy(alpha = 0.22f) else CardBackground,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (active) CyanNeon else SubtleBorder)
                ) {
                    Text(
                        text = label,
                        color = if (active) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        fijosFiltrados.forEach { fijo ->
            Surface(
                color = CardBackground,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.dp,
                    if (fijo.estaPagado) EmeraldGreen.copy(alpha = 0.35f) else SubtleBorder
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(15.dp)) {
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
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = CardElevated,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "Día ${fijo.dia} (Q${fijo.quincena})",
                                        color = CyanNeon,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "${fijo.id} • ${fijo.categoria} • Cuota: $${MainViewModel.formatearNumero(fijo.cuota)}",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }

                        Surface(
                            color = if (fijo.estaPagado) EmeraldGreen.copy(alpha = 0.16f) else RoseRed.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (fijo.estaPagado) "🟢 Pagado" else "🔴 Pendiente",
                                color = if (fijo.estaPagado) EmeraldBright else RoseSoft,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (!fijo.estaPagado) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { onPagarFijo(fijo) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CyanGlow,
                                contentColor = DeepObsidian
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            Icon(Icons.Default.CheckCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            val montoPend = if (fijo.saldo > 0) fijo.saldo else fijo.cuota
                            Text(
                                text = "Registrar Pago ($${MainViewModel.formatearNumero(montoPend)} COP)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
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
    onSendEmailAudit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Selector de módulo secundario
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf(
                0 to "🤖 CFO IA",
                1 to "🏦 Colanta & 2026",
                2 to "📬 Regla .ZIP"
            )
            tabs.forEach { (idx, title) ->
                val active = subTab == idx
                Surface(
                    onClick = { onSelectSubTab(idx) },
                    modifier = Modifier.weight(1f),
                    color = if (active) CyanGlow.copy(alpha = 0.22f) else CardBackground,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (active) CyanNeon else SubtleBorder)
                ) {
                    Text(
                        text = title,
                        color = if (active) CyanNeon else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (active) FontWeight.ExtraBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    )
                }
            }
        }

        when (subTab) {
            0 -> CfoIaSection(
                mensajes = uiState.mensajesCfo,
                onSendPrompt = onSendCfoPrompt
            )
            1 -> ColantaYAnualSection(uiState = uiState)
            2 -> ReglaGmailZipSection(
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
        "¿Cuánto tengo disponible?",
        "Ver Fondos Colanta",
        "¿Qué fijos faltan?",
        "Escanear correos .ZIP"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            sugerencias.forEach { sug ->
                Surface(
                    onClick = { onSendPrompt(sug) },
                    color = CardElevated,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, VioletAccent.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = sug,
                        color = Color(0xFFC4B5FD),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
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
            Button(
                onClick = {
                    if (inputTexto.isNotBlank()) {
                        onSendPrompt(inputTexto)
                        inputTexto = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepObsidian),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Enviar")
            }
        }
    }
}

@Composable
private fun ColantaYAnualSection(uiState: DashboardUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Patrimonio en Fondos Cooperativa Colanta
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, AmberGold.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "PATRIMONIO RESERVADO EN COOPERATIVA COLANTA",
                    color = AmberGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$ ${MainViewModel.formatearNumero(uiState.totalFondosColanta)} COP",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black
                )
                Spacer(modifier = Modifier.height(12.dp))
                FondoRow("🎄 Ahorro Navideño (Se entrega 1 Dic)", uiState.ahorroNavideno, AmberGold)
                FondoRow("🤝 Aportes Sociales Cooperativos", uiState.aportesCooperativos, CyanNeon)
                FondoRow("📈 Ahorro Ordinario Acumulado", uiState.ahorroOrdinario, EmeraldBright)
                FondoRow("🛡️ Otros Fondos y Reservas", uiState.otrosAhorros, TextSecondary)
            }
        }

        // Evolución Anual 2026
        Surface(
            color = CardBackground,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, SubtleBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TRAYECTORIA ANUAL 2026 (CONSOLIDADO)",
                    color = CyanNeon,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(8.dp))
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
                        label = "Superávit Neto",
                        value = "$${MainViewModel.formatearNumero(uiState.superavitAnual)}",
                        valueColor = CyanNeon
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                uiState.resumenAnual.forEach { mes ->
                    Column(modifier = Modifier.padding(vertical = 6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = mes.nombre,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ahorro: $${MainViewModel.formatearNumero(mes.ahorro)} (${mes.tasaAhorro}%)",
                                color = EmeraldBright,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = { (mes.tasaAhorro / 100f).coerceIn(0.05f, 1f) },
                            color = if (mes.tasaAhorro >= 30) EmeraldBright else CyanNeon,
                            trackColor = CardElevated,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun FondoRow(label: String, valor: Long, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
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
            shape = RoundedCornerShape(18.dp),
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

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = onScanGmail,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepObsidian),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.FolderZip, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isScanning) "Analizando Bandeja y .ZIP..." else "Ejecutar Analizador Gmail (.ZIP) Ahora",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp
                    )
                }

                OutlinedButton(
                    onClick = onSendEmailAudit,
                    border = BorderStroke(1.dp, EmeraldGreen.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Registrar Movimiento en Sheets",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Selector Gasto / Ingreso
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val esGasto = tipo == "Gasto"
                Button(
                    onClick = {
                        tipo = "Gasto"
                        categoria = "Alimentación"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (esGasto) RoseRed else CardElevated,
                        contentColor = if (esGasto) Color.White else TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("- Gasto", fontWeight = FontWeight.ExtraBold)
                }

                Button(
                    onClick = {
                        tipo = "Ingreso"
                        categoria = "Salario"
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!esGasto) EmeraldGreen else CardElevated,
                        contentColor = if (!esGasto) DeepObsidian else TextSecondary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("+ Ingreso", fontWeight = FontWeight.ExtraBold)
                }
            }

            OutlinedTextField(
                value = montoTexto,
                onValueChange = { montoTexto = it.filter { ch -> ch.isDigit() } },
                label = { Text("Monto en COP ($)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Chips rápidos de valor
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(5000L to "+$5k", 10000L to "+$10k", 20000L to "+$20k", 50000L to "+$50k").forEach { (inc, label) ->
                    Surface(
                        onClick = {
                            val actual = montoTexto.toLongOrNull() ?: 0L
                            montoTexto = (actual + inc).toString()
                        },
                        color = CardElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SubtleBorder),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = label,
                            color = CyanNeon,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            OutlinedTextField(
                value = descripcion,
                onValueChange = { descripcion = it },
                label = { Text("Concepto o Comercio (ej: Almuerzo Colanta)") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text("Categoría", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val cats = if (tipo == "Gasto") categoriasGasto else categoriasIngreso
                cats.forEach { c ->
                    val sel = categoria == c
                    Surface(
                        onClick = { categoria = c },
                        color = if (sel) CyanGlow.copy(alpha = 0.22f) else CardElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (sel) CyanNeon else SubtleBorder)
                    ) {
                        Text(
                            text = "${iconoPorCategoria(c)} $c",
                            color = if (sel) CyanNeon else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Text("Cuenta de Origen / Destino", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cuentas.forEach { cta ->
                    val sel = cuenta == cta
                    Surface(
                        onClick = { cuenta = cta },
                        color = if (sel) EmeraldGreen.copy(alpha = 0.22f) else CardElevated,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (sel) EmeraldBright else SubtleBorder)
                    ) {
                        Text(
                            text = cta,
                            color = if (sel) EmeraldBright else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Button(
                onClick = {
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
                colors = ButtonDefaults.buttonColors(containerColor = CyanGlow, contentColor = DeepObsidian),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Guardar en Fila 5 de Google Sheets", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
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
                        Text("Quincena 2 (Día 30)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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

private fun iconoPorCategoria(cat: String): String {
    val c = cat.lowercase(Locale.ROOT)
    return when {
        c.contains("alimenta") || c.contains("comida") || c.contains("rest") -> "🍽️"
        c.contains("moto") || c.contains("gasolina") -> "🏍️"
        c.contains("servicio") || c.contains("epm") -> "⚡"
        c.contains("vivienda") || c.contains("arriendo") -> "🏠"
        c.contains("compra") || c.contains("ropa") -> "🛍️"
        c.contains("transporte") || c.contains("taxi") -> "🚕"
        c.contains("antojo") || c.contains("hormiga") || c.contains("dulce") -> "🍫"
        c.contains("salud") -> "💊"
        c.contains("entretenimiento") || c.contains("suscrip") -> "🎮"
        c.contains("salario") || c.contains("nomina") || c.contains("nómina") -> "💰"
        else -> "💳"
    }
}
