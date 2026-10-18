package com.example.appfinanzas.di

import com.example.appfinanzas.data.repository.FinanceRepository
import com.example.appfinanzas.data.repository.FinanceRepositoryImpl
import com.example.appfinanzas.data.repository.GeminiRepository
import com.example.appfinanzas.data.repository.GeminiRepositoryImpl
import com.example.appfinanzas.data.repository.GoogleDriveSyncRepository
import com.example.appfinanzas.data.repository.GoogleDriveSyncRepositoryImpl
import com.example.appfinanzas.data.repository.PardoFinanzasRepositoryImpl
import com.example.appfinanzas.domain.repository.PardoFinanzasRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindFinanceRepository(
        impl: FinanceRepositoryImpl
    ): FinanceRepository

    @Binds
    @Singleton
    abstract fun bindPardoFinanzasRepository(
        impl: PardoFinanzasRepositoryImpl
    ): PardoFinanzasRepository

    @Binds
    @Singleton
    abstract fun bindGeminiRepository(
        impl: GeminiRepositoryImpl
    ): GeminiRepository

    @Binds
    @Singleton
    abstract fun bindGoogleDriveSyncRepository(
        impl: GoogleDriveSyncRepositoryImpl
    ): GoogleDriveSyncRepository
}
