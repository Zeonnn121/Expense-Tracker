package com.example.expensetracker.data
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow


data class MerchantSpending(val merchant: String?, val total: BigDecimal)
@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(tx: TransactionEntity)
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp BETWEEN :monthStart AND :monthEnd")
    suspend fun getMonthlyIncome(monthStart: Long, monthEnd: Long): BigDecimal?

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp BETWEEN :monthStart AND :monthEnd")
    suspend fun getMonthlyExpense(monthStart: Long, monthEnd: Long): BigDecimal?

    @Query("""
    SELECT merchant, SUM(amount) as total FROM transactions
    WHERE type = 'EXPENSE' AND merchant IS NOT NULL
    GROUP BY merchant ORDER BY total DESC LIMIT 10
""")
    suspend fun getSpendingByMerchant(): List<MerchantSpending>
    @Query("""
        SELECT SUM(amount) FROM transactions 
        WHERE type = 'EXPENSE' 
        AND timestamp BETWEEN :monthStart AND :monthEnd
    """)
    suspend fun getMonthlyTotal(monthStart: Long, monthEnd: Long): BigDecimal?
}