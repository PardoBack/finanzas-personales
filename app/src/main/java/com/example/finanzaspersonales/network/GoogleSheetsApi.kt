package com.example.finanzaspersonales.network

import com.example.finanzaspersonales.data.Transaction
import com.google.gson.JsonObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

data class SyncRequest(
    val action: String = "syncTransactions",
    val transactions: List<Transaction>,
    val token: String = RetrofitClient.AUTH_TOKEN
)

data class SyncResponse(
    val status: String? = null,
    val ok: Boolean? = null,
    val syncedIds: List<String>? = null,
    val mensaje: String? = null
)

interface GoogleSheetsApi {
    @POST("exec")
    suspend fun syncTransactions(@Body request: SyncRequest): Response<SyncResponse>

    @GET("exec")
    suspend fun getDashboardData(
        @Query("action") action: String = "getData",
        @Query("mes") mes: String? = null,
        @Query("anio") anio: Int? = 2026,
        @Query("token") token: String = RetrofitClient.AUTH_TOKEN
    ): Response<JsonObject>

    @GET("exec")
    suspend fun executeGetAction(
        @Query("action") action: String,
        @Query("mes") mes: String? = null,
        @Query("anio") anio: Int? = null,
        @Query("token") token: String = RetrofitClient.AUTH_TOKEN
    ): Response<JsonObject>

    @POST("exec")
    suspend fun executePostAction(
        @Body payload: Map<String, @JvmSuppressWildcards Any?>
    ): Response<JsonObject>
}
