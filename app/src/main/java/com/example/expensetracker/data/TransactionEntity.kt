package com.example.expensetracker.data
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val amount: BigDecimal,
    val type: String,
    val merchant: String?,
    val bankName: String,
    val timestamp: Long
)