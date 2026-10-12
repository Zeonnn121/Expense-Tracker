package com.example.expensetracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppViewModel
import com.example.expensetracker.data.TransactionEntity
import java.text.SimpleDateFormat
import java.util.*

// ─────────────────────────────────────
// FILTER ENUMS
// ─────────────────────────────────────

private enum class DateFilter(val label: String, val days: Int?) {
    ALL("All Time", null),
    THIS_MONTH("This Month", 0),      // special-cased below
    LAST_30("Last 30 Days", 30),
    LAST_90("Last 90 Days", 90),
}

private enum class AmountFilter(val label: String) {
    ALL("All"),
    UP_TO_200("Up to 200"),
    V200_500("200 – 500"),
    V500_2000("500 – 2000"),
    V2000_PLUS("2000+"),
}

private enum class TypeFilter(val label: String) {
    ALL("All"),
    SENT("Money Sent"),
    RECEIVED("Money Received"),
}

// ═════════════════════════════════════════════════════
// TRANSACTIONS SCREEN
// ═════════════════════════════════════════════════════

@Composable
fun TransactionsScreen(
    viewModel: AppViewModel,
    onTransactionClick: (String) -> Unit
) {
    val allTxns by viewModel.allTransactions.collectAsState()
    val currency by viewModel.currency.collectAsState()

    var dateFilter by remember { mutableStateOf(DateFilter.THIS_MONTH) }
    var amountFilter by remember { mutableStateOf(AmountFilter.ALL) }
    var typeFilter by remember { mutableStateOf(TypeFilter.ALL) }
    var txnToDelete by remember { mutableStateOf<TransactionEntity?>(null) }

    // Apply filters
    val filtered = remember(allTxns, dateFilter, amountFilter, typeFilter) {
        val now = System.currentTimeMillis()

        val dateStart: Long = when (dateFilter) {
            DateFilter.ALL -> 0L
            DateFilter.THIS_MONTH -> {
                val cal = Calendar.getInstance()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> now - (dateFilter.days!! * 24 * 60 * 60 * 1000L)
        }

        allTxns.filter { txn ->
            // Date
            txn.timestamp >= dateStart &&
            // Amount
            when (amountFilter) {
                AmountFilter.ALL -> true
                AmountFilter.UP_TO_200 -> txn.amount.toDouble() <= 200
                AmountFilter.V200_500 -> txn.amount.toDouble() in 200.0..500.0
                AmountFilter.V500_2000 -> txn.amount.toDouble() in 500.0..2000.0
                AmountFilter.V2000_PLUS -> txn.amount.toDouble() > 2000
            } &&
            // Type
            when (typeFilter) {
                TypeFilter.ALL -> true
                TypeFilter.SENT -> txn.type == "EXPENSE"
                TypeFilter.RECEIVED -> txn.type == "INCOME"
            }
        }
    }

    // Group by year+month, newest first
    val grouped: List<Pair<Pair<Int, Int>, List<TransactionEntity>>> =
        remember(filtered) {
            filtered
                .groupBy { txn ->
                    val cal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                    cal.get(Calendar.YEAR) to cal.get(Calendar.MONTH)
                }
                .entries
                .sortedByDescending { (ym, _) -> ym.first * 100 + ym.second }
                .map { it.key to it.value }
        }

    Column(modifier = Modifier.fillMaxSize()) {

        // ─────────────────────────────────────
        // FILTER CHIPS ROW
        // ─────────────────────────────────────

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterDropdown(
                    label = "Date",
                    selected = dateFilter.label,
                    options = DateFilter.entries.map { it.label },
                    onSelect = { label -> dateFilter = DateFilter.entries.first { it.label == label } }
                )
            }
            item {
                FilterDropdown(
                    label = "Amount",
                    selected = amountFilter.label,
                    options = AmountFilter.entries.map { it.label },
                    onSelect = { label -> amountFilter = AmountFilter.entries.first { it.label == label } }
                )
            }
            item {
                FilterDropdown(
                    label = "Type",
                    selected = typeFilter.label,
                    options = TypeFilter.entries.map { it.label },
                    onSelect = { label -> typeFilter = TypeFilter.entries.first { it.label == label } }
                )
            }
        }

        HorizontalDivider()

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No transactions",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return@Column
        }

        // ─────────────────────────────────────
        // GROUPED TRANSACTION LIST
        // ─────────────────────────────────────

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            grouped.forEach { (yearMonth, txns) ->
                val (year, month) = yearMonth
                val cal = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                }
                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(cal.time)

                // Month net (income - expense)
                val netForMonth = txns.sumOf { txn ->
                    when (txn.type) {
                        "INCOME" -> txn.amount.toDouble()
                        else -> -txn.amount.toDouble()
                    }
                }
                val netSign = if (netForMonth >= 0) "+" else ""
                val netColor = if (netForMonth >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)

                // Month header — year small, month large, net amount
                item(key = "header_${year}_$month") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Text(
                            year.toString(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                monthName,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "$netSign$currency${"%.2f".format(netForMonth)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = netColor
                            )
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Transaction rows
                items(txns, key = { it.id }) { txn ->
                    TxnRow(
                        txn = txn,
                        currency = currency,
                        onClick = { onTransactionClick(txn.merchant ?: txn.bankName) },
                        onLongPress = { txnToDelete = txn }
                    )
                }

                item(key = "spacer_${year}_$month") {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }

    // Delete confirm dialog
    txnToDelete?.let { txn ->
        AlertDialog(
            onDismissRequest = { txnToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete Transaction") },
            text = { Text("Delete this transaction? This cannot be undone and will adjust your balance.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteTransaction(txn); txnToDelete = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                OutlinedButton(onClick = { txnToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

// ═════════════════════════════════════════════════════
// FILTER DROPDOWN CHIP
// ═════════════════════════════════════════════════════

@Composable
private fun FilterDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = selected != options.first()   // first option = "All" / default

    Box {
        FilterChip(
            selected = isActive,
            onClick = { expanded = true },
            label = {
                Text(
                    if (isActive) selected else label,
                    maxLines = 1
                )
            },
            trailingIcon = {
                Icon(
                    if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            },
            shape = RoundedCornerShape(50)
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                    leadingIcon = if (option == selected) ({
                        Text("✓", color = MaterialTheme.colorScheme.primary)
                    }) else null
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════
// TRANSACTION ROW (avatar + name + date + amount)
// ═════════════════════════════════════════════════════

private val IncomeGreen = Color(0xFF2E7D32)
private val ExpenseRed = Color(0xFFC62828)

// Random-ish but stable pastel colours keyed on first char
private val avatarColors = listOf(
    Color(0xFF388E3C), Color(0xFF1976D2), Color(0xFF7B1FA2),
    Color(0xFFE64A19), Color(0xFF00838F), Color(0xFFF57F17),
    Color(0xFFAD1457), Color(0xFF00695C)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TxnRow(
    txn: TransactionEntity,
    currency: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    val isIncome = txn.type == "INCOME"
    val label = txn.merchant ?: txn.bankName
    val initial = label.trim().firstOrNull()?.uppercaseChar() ?: '?'
    val avatarColor = avatarColors[(initial.code) % avatarColors.size]

    val dateStr = remember(txn.timestamp) {
        SimpleDateFormat("dd MMMM", Locale.getDefault()).format(Date(txn.timestamp))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongPress)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                initial.toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        // Name + date
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(dateStr, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        // Amount
        val amountStr = "${if (isIncome) "+ " else ""}$currency${"%.0f".format(txn.amount)}"
        Text(
            amountStr,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = if (isIncome) IncomeGreen else MaterialTheme.colorScheme.onSurface
        )
    }
}
