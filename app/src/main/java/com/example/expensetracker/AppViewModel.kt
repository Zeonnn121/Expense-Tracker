package com.example.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.TransactionEntity
import com.example.expensetracker.data.UserPreferences
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar
import java.util.Locale

class AppViewModel(private val app: ExpenseApp) : ViewModel() {

    private val prefs = UserPreferences(app)

    val userName: StateFlow<String> = prefs.name.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userEmail: StateFlow<String> = prefs.email.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val currency: StateFlow<String> = prefs.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "₹")

    private val baseBalance: StateFlow<Double> = prefs.balance.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val allTransactions: StateFlow<List<TransactionEntity>> =
        app.db.transactionDao().getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val balance: StateFlow<Double> = combine(baseBalance, allTransactions) { base, txns ->
        base + txns.sumOf { txn ->
            when (txn.type) {
                "INCOME" -> txn.amount.toDouble()
                "EXPENSE" -> -txn.amount.toDouble()
                else -> 0.0
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    private val _monthlyIncome = MutableStateFlow(BigDecimal.ZERO)
    val monthlyIncome: StateFlow<BigDecimal> = _monthlyIncome

    private val _monthlyExpense = MutableStateFlow(BigDecimal.ZERO)
    val monthlyExpense: StateFlow<BigDecimal> = _monthlyExpense

    init { loadMonthlySummary() }

    fun loadMonthlySummary() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val monthStart = cal.timeInMillis
            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            _monthlyIncome.value = app.db.transactionDao().getMonthlyIncome(monthStart, monthEnd) ?: BigDecimal.ZERO
            _monthlyExpense.value = app.db.transactionDao().getMonthlyExpense(monthStart, monthEnd) ?: BigDecimal.ZERO
        }
    }

    fun updateBalance(desiredBalance: Double) {
        viewModelScope.launch {
            // User wants the displayed balance to be exactly desiredBalance.
            // displayed = base + transactionTotal, so base = desired - transactionTotal
            val transactionTotal = allTransactions.value.sumOf { txn ->
                when (txn.type) {
                    "INCOME" -> txn.amount.toDouble()
                    "EXPENSE" -> -txn.amount.toDouble()
                    else -> 0.0
                }
            }
            prefs.setBalance(desiredBalance - transactionTotal)
        }
    }

    fun updateProfile(name: String, email: String, currency: String) {
        viewModelScope.launch {
            prefs.setName(name); prefs.setEmail(email); prefs.setCurrency(currency)
        }
    }

    fun addTransaction(amount: BigDecimal, type: String, merchant: String?, bankName: String) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                amount = amount,
                type = type,
                merchant = merchant?.ifBlank { null },
                bankName = bankName.ifBlank { "Manual" },
                timestamp = System.currentTimeMillis()
            )
            app.db.transactionDao().insert(entity)
            loadMonthlySummary()
        }
    }

    fun deleteTransaction(tx: TransactionEntity) {
        viewModelScope.launch {
            app.db.transactionDao().delete(tx)
            loadMonthlySummary()
        }
    }

    fun monthlySpendingLast6Months(): List<Pair<String, Float>> {
        val txns = allTransactions.value.filter { it.type == "EXPENSE" }
        val result = mutableListOf<Pair<String, Float>>()
        for (i in 5 downTo 0) {
            val c = Calendar.getInstance()
            c.add(Calendar.MONTH, -i)
            c.set(Calendar.DAY_OF_MONTH, 1); c.set(Calendar.HOUR_OF_DAY, 0)
            c.set(Calendar.MINUTE, 0); c.set(Calendar.SECOND, 0)
            val start = c.timeInMillis
            val label = java.text.SimpleDateFormat("MMM", Locale.getDefault()).format(c.time)
            c.add(Calendar.MONTH, 1)
            val end = c.timeInMillis
            val total = txns.filter { it.timestamp in start until end }.sumOf { it.amount.toDouble() }.toFloat()
            result.add(label to total)
        }
        return result
    }

    suspend fun topMerchants(): List<Pair<String, Double>> =
        app.db.transactionDao().getSpendingByMerchant().map { (it.merchant ?: "Unknown") to it.total.toDouble() }
}