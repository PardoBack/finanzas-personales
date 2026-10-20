package com.example.finanzaspersonales.data

import android.util.Log
import com.example.finanzaspersonales.network.RetrofitClient
import com.example.finanzaspersonales.network.SyncRequest
import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun syncUnsyncedTransactions() {
        val unsynced = transactionDao.getUnsyncedTransactions()
        if (unsynced.isEmpty()) return

        try {
            val request = SyncRequest(transactions = unsynced)
            val response = RetrofitClient.api.syncTransactions(request)
            
            if (response.isSuccessful && response.body()?.status == "success") {
                val syncedIds = response.body()?.syncedIds ?: unsynced.map { it.id }
                transactionDao.markAsSynced(syncedIds)
            }
        } catch (e: Exception) {
            Log.e("SyncError", "Error syncing transactions", e)
        }
    }
}
