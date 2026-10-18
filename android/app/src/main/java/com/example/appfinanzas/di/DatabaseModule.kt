package com.example.appfinanzas.di

import android.content.Context
import androidx.room.Room
import com.example.appfinanzas.data.local.FinanceDatabase
import com.example.appfinanzas.data.local.dao.FixedExpenseDao
import com.example.appfinanzas.data.local.dao.SavingsGoalDao
import com.example.appfinanzas.data.local.dao.TransactionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinanceDatabase {
        return Room.databaseBuilder(
            context,
            FinanceDatabase::class.java,
            "finance_database"
        ).build()
    }

    @Provides
    fun provideTransactionDao(db: FinanceDatabase): TransactionDao = db.transactionDao()

    @Provides
    fun provideFixedExpenseDao(db: FinanceDatabase): FixedExpenseDao = db.fixedExpenseDao()

    @Provides
    fun provideSavingsGoalDao(db: FinanceDatabase): SavingsGoalDao = db.savingsGoalDao()
}
