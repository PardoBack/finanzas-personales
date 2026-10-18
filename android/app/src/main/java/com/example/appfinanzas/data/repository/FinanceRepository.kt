package com.example.appfinanzas.data.repository

import android.util.Log
import com.example.appfinanzas.data.local.FixedExpenseEntity
import com.example.appfinanzas.data.local.SavingsGoalEntity
import com.example.appfinanzas.data.local.TransactionEntity
import com.example.appfinanzas.data.local.dao.FixedExpenseDao
import com.example.appfinanzas.data.local.dao.SavingsGoalDao
import com.example.appfinanzas.data.local.dao.TransactionDao
import com.example.appfinanzas.data.remote.FinanceApiService
import com.example.appfinanzas.data.remote.GastoFijo
import com.example.appfinanzas.data.remote.Movimiento
import com.example.appfinanzas.data.remote.ResumenResponse
import com.example.appfinanzas.data.remote.SimpleResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface FinanceRepository {
    fun getResumenFlow(): Flow<ResumenResponse>
    fun getMovimientosFlow(): Flow<List<Movimiento>>
    fun getFijosFlow(): Flow<List<GastoFijo>>
    fun getSavingsGoalsFlow(): Flow<List<SavingsGoalEntity>>

    suspend fun checkAndSeedInitialData()
    suspend fun refreshData()
    suspend fun addMovimiento(movimiento: Movimiento): Result<SimpleResponse>
    suspend fun addSavingsGoal(goal: SavingsGoalEntity)
    suspend fun contributeToGoal(goalId: Int, amount: Double)
    suspend fun syncUnsyncedTransactions()

    // Legacy or for direct sync compatibility
    suspend fun getResumen(): Result<ResumenResponse>
    suspend fun getMovimientos(): Result<List<Movimiento>>
    suspend fun getFijos(): Result<List<GastoFijo>>
}

@Singleton
class FinanceRepositoryImpl @Inject constructor(
    private val apiService: FinanceApiService,
    private val transactionDao: TransactionDao,
    private val fixedExpenseDao: FixedExpenseDao,
    private val savingsGoalDao: SavingsGoalDao,
) : FinanceRepository {

    override suspend fun checkAndSeedInitialData() {
        // Kept empty to ensure the app starts 100% clean without personal mock data
    }

    override fun getResumenFlow(): Flow<ResumenResponse> = transactionDao.getAllTransactions().map { transactions ->
        val totalIngresos = transactions.asSequence().filter { it.tipo == "ingreso" }.sumOf { it.monto }.toDouble()
        val totalGastos = transactions.asSequence().filter { it.tipo == "gasto" }.sumOf { it.monto }.toDouble()
        ResumenResponse(
            balance = totalIngresos - totalGastos,
            disponible = totalIngresos - totalGastos,
            ingresos = totalIngresos,
            gastos = totalGastos
        )
    }

    override fun getMovimientosFlow(): Flow<List<Movimiento>> = transactionDao.getAllTransactions().map { entities ->
        entities.map { it.toMovimiento() }
    }

    override fun getFijosFlow(): Flow<List<GastoFijo>> = fixedExpenseDao.getAllFixedExpenses().map { entities ->
        entities.map { it.toGastoFijo() }
    }

    override fun getSavingsGoalsFlow(): Flow<List<SavingsGoalEntity>> = savingsGoalDao.getAllSavingsGoals()

    override suspend fun refreshData() {
        runCatching {
            val movimientosResponse = apiService.getMovimientos()
            if (movimientosResponse.isSuccessful) {
                val remoteMovimientos = movimientosResponse.body() ?: emptyList()
                if (remoteMovimientos.isNotEmpty()) {
                    transactionDao.clearSyncedTransactions()
                    transactionDao.insertTransactions(remoteMovimientos.map { it.toEntity() })
                }
            }

            val fijosResponse = apiService.getFijos()
            if (fijosResponse.isSuccessful) {
                val remoteFijos = fijosResponse.body() ?: emptyList()
                if (remoteFijos.isNotEmpty()) {
                    fixedExpenseDao.clearAll()
                    fixedExpenseDao.insertFixedExpenses(remoteFijos.map { it.toEntity() })
                }
            }
        }.onFailure {
            Log.e("FinanceRepository", "Error refreshing data", it)
        }
    }

    override suspend fun addMovimiento(movimiento: Movimiento): Result<SimpleResponse> = runCatching {
        // Insert locally first as unsynced
        val entity = movimiento.toEntity(isSynced = false)
        transactionDao.insertTransaction(entity)
        
        // Try to sync immediately
        syncUnsyncedTransactions()
        
        SimpleResponse(success = true, message = "Guardado localmente")
    }.onFailure {
        Log.e("FinanceRepository", "Error adding movimiento", it)
    }

    override suspend fun addSavingsGoal(goal: SavingsGoalEntity) {
        savingsGoalDao.insertGoal(goal)
    }

    override suspend fun contributeToGoal(goalId: Int, amount: Double) {
        val goals = savingsGoalDao.getAllSavingsGoals().first()
        goals.find { it.id == goalId }?.let { goal ->
            savingsGoalDao.updateGoal(goal.copy(currentAmount = goal.currentAmount + amount))
        }
    }

    override suspend fun syncUnsyncedTransactions() {
        val unsynced = transactionDao.getUnsyncedTransactions()
        unsynced.forEach { entity ->
            runCatching {
                val response = apiService.nuevoMovimiento(movimiento = entity.toMovimiento())
                if (response.isSuccessful) {
                    transactionDao.updateTransaction(entity.copy(isSynced = true))
                }
            }
        }
    }

    // Legacy implementations
    override suspend fun getResumen(): Result<ResumenResponse> = Result.success(getResumenFlow().first())
    override suspend fun getMovimientos(): Result<List<Movimiento>> = Result.success(getMovimientosFlow().first())
    override suspend fun getFijos(): Result<List<GastoFijo>> = Result.success(getFijosFlow().first())

    // Helper extensions
    private fun Movimiento.toEntity(isSynced: Boolean = true): TransactionEntity = TransactionEntity(
        remoteId = id,
        fecha = fecha,
        concepto = concepto,
        monto = monto,
        tipo = tipo,
        categoria = categoria,
        cuenta = cuenta,
        observaciones = observaciones,
        isSynced = isSynced
    )

    private fun TransactionEntity.toMovimiento(): Movimiento = Movimiento(
        id = remoteId,
        fecha = fecha,
        concepto = concepto,
        monto = monto,
        tipo = tipo,
        categoria = categoria,
        cuenta = cuenta,
        observaciones = observaciones
    )

    private fun GastoFijo.toEntity(): FixedExpenseEntity = FixedExpenseEntity(
        id = id,
        concepto = concepto,
        monto = monto,
        diaPago = diaPago
    )

    private fun FixedExpenseEntity.toGastoFijo(): GastoFijo = GastoFijo(
        id = id,
        concepto = concepto,
        monto = monto,
        diaPago = diaPago
    )
}
