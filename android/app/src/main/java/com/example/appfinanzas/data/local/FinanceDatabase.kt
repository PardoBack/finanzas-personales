package com.example.appfinanzas.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.appfinanzas.data.local.dao.FixedExpenseDao
import com.example.appfinanzas.data.local.dao.SavingsGoalDao
import com.example.appfinanzas.data.local.dao.TransactionDao

@Database(
    entities = [TransactionEntity::class, FixedExpenseEntity::class, SavingsGoalEntity::class],
    version = 1,
    exportSchema = false
)
abstract class FinanceDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun savingsGoalDao(): SavingsGoalDao
}
