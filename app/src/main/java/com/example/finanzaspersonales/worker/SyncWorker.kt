package com.example.finanzaspersonales.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.finanzaspersonales.data.AppDatabase
import com.example.finanzaspersonales.data.TransactionRepository

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = TransactionRepository(database.transactionDao())

        return try {
            repository.syncUnsyncedTransactions()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
