package com.example.appfinanzas.domain.model

enum class MovementType {
    INGRESO,
    EGRESO,
    TRANSFERENCIA_INTERNA
}

enum class FixedExpenseStatus {
    PAGADO,
    PENDIENTE,
    PARCIAL,
    VENCIDO
}

/**
 * Modelo de Movimiento alineado con las 14 columnas de MOVIMIENTOS en V25.3 PRO:
 * ID, Fecha, Hora, Tipo, Categoría, Subcategoría, Descripción, Valor, Método, Cuenta, Mes, Año, Origen, Referencia.
 */
data class PardoMovement(
    val id: String = "",
    val fecha: String = "",
    val hora: String = "",
    val tipo: MovementType = MovementType.EGRESO,
    val categoria: String = "",
    val subcategoria: String = "",
    val descripcion: String = "",
    val valor: Double = 0.0,
    val metodoPago: String = "",
    val cuenta: String = "",
    val mes: String = "",
    val ano: String = "",
    val origen: String = "Android App",
    val referencia: String = "",
    val observaciones: String = "",
    val blockId: String = ""
)

/**
 * Modelo de Gasto Fijo alineado con las 8 columnas de CONTROL FIJOS en V25.3 PRO:
 * ID, Concepto, Categoría, Día de cobro, Valor mensual, Último mes pagado, Fecha último pago, Notas/Deuda.
 */
data class PardoFixedExpense(
    val id: String = "",
    val concepto: String = "",
    val categoria: String = "",
    val diaCobro: Int = 1,
    val valorMensual: Double = 0.0,
    val ultimoMesPagado: String = "",
    val fechaUltimoPago: String = "",
    val notasDeuda: String = "",
    val cuotaBase: Double = 0.0,
    val abonado: Double = 0.0,
    val estado: FixedExpenseStatus = FixedExpenseStatus.PENDIENTE,
    val prioridad: Int = 1,
    val medioPago: String = ""
)

data class BudgetAlert(
    val categoria: String = "",
    val limite: Double = 0.0,
    val gastado: Double = 0.0,
    val isOverdraft: Boolean = false
)

data class DashboardStatus(
    val disponible: Double = 0.0,
    val budgetAlerts: List<BudgetAlert> = emptyList(),
    val fixedExpenses: List<PardoFixedExpense> = emptyList()
)
