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

    val balance: StateFlow<Double> = prefs.balance.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)
    val userName: StateFlow<String> = prefs.name.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val userEmail: StateFlow<String> = prefs.email.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val currency: StateFlow<String> = prefs.currency.stateIn(viewModelScope, SharingStarted.Eagerly, "₹")

    val allTransactions: StateFlow<List<TransactionEntity>> =
        app.db.transactionDao().getAllTransactions()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

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

    fun updateBalance(value: Double) { viewModelScope.launch { prefs.setBalance(value) } }

    fun updateProfile(name: String, email: String, currency: String) {
        viewModelScope.launch {
            prefs.setName(name); prefs.setEmail(email); prefs.setCurrency(currency)
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