package com.example.expensetracker.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.math.BigDecimal

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(tx: TransactionEntity)

    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' 
        AND timestamp BETWEEN :monthStart AND :monthEnd
    """)
    suspend fun getMonthlyTotal(monthStart: Long, monthEnd: Long): BigDecimal?
}