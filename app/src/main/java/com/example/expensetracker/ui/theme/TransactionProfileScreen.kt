package com.example.expensetracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import java.util.Calendar
import java.util.Date
import java.util.Locale

private val IncomeGreen = Color(0xFF2E7D32)
private val ExpenseRed = Color(0xFFC62828)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionProfileScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {

    val counterparty by viewModel.selectedCounterparty.collectAsState()
    val transactions by viewModel.counterpartyTransactions.collectAsState()
    val currency by viewModel.currency.collectAsState()

    val name = counterparty ?: ""

    // Totals shared with this counterparty
    val totalSent = transactions
        .filter { it.type != "INCOME" }
        .sumOf { it.amount.toDouble() }

    val totalReceived = transactions
        .filter { it.type == "INCOME" }
        .sumOf { it.amount.toDouble() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Avatar(name)

                        Column {
                            Text(
                                name.ifBlank { "Transaction" },
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                "${transactions.size} transactions",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { scaffoldPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
        ) {

            // ─────────────────────────────────────
            // SUMMARY CARD (paid vs received)
            // ─────────────────────────────────────

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "You sent",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$currency${"%.2f".format(totalSent)}",
                            fontWeight = FontWeight.SemiBold,
                            color = ExpenseRed
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "You received",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$currency${"%.2f".format(totalReceived)}",
                            fontWeight = FontWeight.SemiBold,
                            color = IncomeGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.padding(8.dp))

            // ─────────────────────────────────────
            // TRANSACTION HISTORY (chat style)
            //
            // reverseLayout + newest-first list
            // → newest bubble at the bottom,
            //   starts scrolled to the latest txn
            // ─────────────────────────────────────

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                reverseLayout = true,
                contentPadding = PaddingValues(
                    horizontal = 16.dp, vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                itemsIndexed(
                    transactions,
                    key = { _, txn -> txn.id }
                ) { index, txn ->

                    val isIncome = txn.type == "INCOME"

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment =
                            if (isIncome) Alignment.Start else Alignment.End
                    ) {

                        // Date divider when the day changes.
                        // In a reversed list the visually-next item
                        // is the older one at index + 1
                        if (
                            index == transactions.lastIndex ||
                            !isSameDay(
                                txn.timestamp,
                                transactions[index + 1].timestamp
                            )
                        ) {
                            DateChip(txn.timestamp)
                        }

                        TransactionBubble(
                            txn = txn,
                            currency = currency,
                            isIncome = isIncome
                        )
                    }
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════
// BUBBLE — outgoing right, incoming left
// ═════════════════════════════════════════════════════

@Composable
private fun TransactionBubble(
    txn: TransactionEntity,
    currency: String,
    isIncome: Boolean
) {

    val accent = if (isIncome) IncomeGreen else ExpenseRed

    val timeStr = remember(txn.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
            .format(Date(txn.timestamp))
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement =
            if (isIncome) Arrangement.Start else Arrangement.End
    ) {

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = if (isIncome) 4.dp else 16.dp,
                        topEnd = if (isIncome) 16.dp else 4.dp,
                        bottomStart = 16.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(accent.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End
        ) {

            Text(
                "${if (isIncome) "+" else "-"}$currency${"%.2f".format(txn.amount)}",
                color = accent,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.align(Alignment.Start)
            )

            txn.merchant?.let {
                Text(
                    "via ${txn.bankName}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Text(
                timeStr,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═════════════════════════════════════════════════════
// DATE CHIP — Today / Yesterday / dd MMM yyyy
// ═════════════════════════════════════════════════════

@Composable
private fun DateChip(timestamp: Long) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {

        Text(
            text = remember(timestamp) { dayLabel(timestamp) },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

// ═════════════════════════════════════════════════════
// AVATAR — first letter of the counterparty name
// ═════════════════════════════════════════════════════

@Composable
private fun Avatar(name: String) {

    val initial = name.trim().firstOrNull()?.uppercaseChar() ?: '?'

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            initial.toString(),
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ─────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────

private fun isSameDay(a: Long, b: Long): Boolean {

    val ca = Calendar.getInstance().apply { timeInMillis = a }
    val cb = Calendar.getInstance().apply { timeInMillis = b }

    return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR) &&
            ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR)
}

private fun dayLabel(timestamp: Long): String {

    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

    fun matches(other: Calendar) =
        cal.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

    return when {
        matches(today) -> "Today"
        matches(yesterday) -> "Yesterday"
        else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            .format(Date(timestamp))
    }
}
