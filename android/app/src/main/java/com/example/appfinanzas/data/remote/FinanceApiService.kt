package com.example.appfinanzas.data.remote

import kotlinx.serialization.Serializable
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface FinanceApiService {
    @GET("exec")
    suspend fun getResumen(
        @Query("accion") accion: String = "resumen"
    ): Response<ResumenResponse>

    @GET("exec")
    suspend fun getMovimientos(
        @Query("accion") accion: String = "movimientos"
    ): Response<List<Movimiento>>

    @GET("exec")
    suspend fun getFijos(
        @Query("accion") accion: String = "fijos"
    ): Response<List<GastoFijo>>

    @GET("exec")
    suspend fun getDisponible(
        @Query("accion") accion: String = "disponible"
    ): Response<DisponibleResponse>

    @GET("exec")
    suspend fun getFugas(
        @Query("accion") accion: String = "fugas"
    ): Response<FugasResponse>

    @POST("exec")
    suspend fun nuevoMovimiento(
        @Query("accion") accion: String = "nuevo_movimiento",
        @Body movimiento: Movimiento
    ): Response<SimpleResponse>

    @POST("exec")
    suspend fun pagarFijo(
        @Query("accion") accion: String = "pagar_fijo",
        @Body payload: PagarFijoPayload
    ): Response<SimpleResponse>

    @POST("exec")
    suspend fun borrarUltimo(
        @Query("accion") accion: String = "borrar_ultimo"
    ): Response<SimpleResponse>
}

@Serializable
data class ResumenResponse(
    val balance: Double = 0.0,
    val disponible: Double = 0.0,
    val ingresos: Double = 0.0,
    val gastos: Double = 0.0
)

@Serializable
data class DisponibleResponse(
    val disponibleReal: Double = 0.0,
    val fechaConsulta: String = ""
)

@Serializable
data class FugaDetalle(
    val id: String = "",
    val descripcion: String = "",
    val valor: Double = 0.0,
    val fecha: String = ""
)

@Serializable
data class FugasResponse(
    val totalFugasHormiga: Double = 0.0,
    val cantidadRegistros: Int = 0,
    val detalles: List<FugaDetalle> = emptyList()
)

/**
 * Representa la estructura de 14 columnas de MOVIMIENTOS:
 * ID, Fecha, Hora, Tipo, Categoría, Subcategoría, Descripción, Valor, Método, Cuenta, Mes, Año, Origen, Referencia.
 */
@Serializable
data class Movimiento(
    val id: String? = null,
    val fecha: String = "",
    val hora: String = "",
    val tipo: String = "EGRESO",
    val categoria: String = "",
    val subcategoria: String = "",
    val descripcion: String = "",
    val valor: Double = 0.0,
    val metodo: String = "",
    val cuenta: String = "Efectivo",
    val mes: String = "",
    val ano: String = "",
    val origen: String = "Android App",
    val referencia: String = ""
) {
    val concepto: String get() = descripcion.ifEmpty { "Movimiento" }
    val monto: Int get() = valor.toInt()
    val observaciones: String? get() = referencia.ifEmpty { null }

    constructor(
        id: Any? = null,
        fecha: String = "",
        hora: String = "",
        tipo: String = "EGRESO",
        categoria: String = "",
        subcategoria: String = "",
        descripcion: String = "",
        concepto: String = "",
        valor: Double = 0.0,
        monto: Number = 0,
        metodo: String = "",
        cuenta: String = "Efectivo",
        mes: String = "",
        ano: String = "",
        origen: String = "Android App",
        referencia: String = "",
        observaciones: String? = null
    ) : this(
        id = id?.toString(),
        fecha = fecha,
        hora = hora,
        tipo = tipo,
        categoria = categoria,
        subcategoria = subcategoria,
        descripcion = if (descripcion.isNotEmpty()) descripcion else concepto,
        valor = if (valor != 0.0) valor else monto.toDouble(),
        metodo = metodo,
        cuenta = cuenta,
        mes = mes,
        ano = ano,
        origen = if (origen.isNotEmpty()) origen else "Android App",
        referencia = if (referencia.isNotEmpty()) referencia else (observaciones ?: "")
    )
}

/**
 * Representa la estructura de 8 columnas de CONTROL FIJOS:
 * ID, Concepto, Categoría, Día de cobro, Valor mensual, Último mes pagado, Fecha último pago, Notas/Deuda.
 */
@Serializable
data class GastoFijo(
    val id: String = "",
    val concepto: String = "",
    val categoria: String = "",
    val diaCobro: Int = 1,
    val valorMensual: Double = 0.0,
    val ultimoMesPagado: String = "",
    val fechaUltimoPago: String = "",
    val notasDeuda: String = ""
) {
    val monto: Double get() = valorMensual
    val diaPago: Int get() = diaCobro

    constructor(
        id: Any = "",
        concepto: String = "",
        categoria: String = "",
        diaCobro: Int = 1,
        diaPago: Int = 1,
        valorMensual: Double = 0.0,
        monto: Double = 0.0,
        ultimoMesPagado: String = "",
        fechaUltimoPago: String = "",
        notasDeuda: String = ""
    ) : this(
        id = id.toString(),
        concepto = concepto,
        categoria = categoria,
        diaCobro = if (diaCobro != 1) diaCobro else diaPago,
        valorMensual = if (valorMensual != 0.0) valorMensual else monto,
        ultimoMesPagado = ultimoMesPagado,
        fechaUltimoPago = fechaUltimoPago,
        notasDeuda = notasDeuda
    )
}

@Serializable
data class PagarFijoPayload(
    val idFijo: String,
    val monto: Double = 0.0,
    val metodoPago: String = "Nequi",
    val cuenta: String = "Nequi",
    val periodo: String = ""
)

@Serializable
data class SimpleResponse(
    val success: Boolean = true,
    val message: String = ""
)
