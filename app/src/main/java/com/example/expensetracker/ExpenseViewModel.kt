package com.example.expensetracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.Calendar

class ExpenseViewModel(private val app: ExpenseApp) : ViewModel() {

    private val _monthlyTotal = MutableStateFlow(BigDecimal.ZERO)
    val monthlyTotal: StateFlow<BigDecimal> = _monthlyTotal

    init {
        loadMonthlyTotal()
    }

    fun loadMonthlyTotal() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val monthStart = cal.timeInMillis

            cal.add(Calendar.MONTH, 1)
            val monthEnd = cal.timeInMillis

            val total = app.db.transactionDao().getMonthlyTotal(monthStart, monthEnd)
            _monthlyTotal.value = total ?: BigDecimal.ZERO
        }
    }
}