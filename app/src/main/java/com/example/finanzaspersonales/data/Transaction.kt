package com.example.finanzaspersonales.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: String,
    val concept: String,
    val category: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
