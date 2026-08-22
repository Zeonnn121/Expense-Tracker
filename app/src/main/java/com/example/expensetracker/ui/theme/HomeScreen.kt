package com.example.expensetracker.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppViewModel
import com.example.expensetracker.data.TransactionEntity
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onTransactionClick: (String) -> Unit
) {

    val balance by viewModel.balance.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val expense by viewModel.monthlyExpense.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.currency.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }

    // Transaction selected for deletion after long press
    var txnToDelete by remember {
        mutableStateOf<TransactionEntity?>(null)
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    showAddDialog = true
                }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Transaction"
                )
            }
        }
    ) { scaffoldPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(16.dp)
        ) {

            // ─────────────────────────────────────
            // BALANCE CARD
            // ─────────────────────────────────────

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {

                    Text(
                        "Available Balance",
                        fontSize = 14.sp
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    Text(
                        "$currency${"%.2f".format(balance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            showBalanceDialog = true
                        }
                    )

                    Spacer(
                        modifier = Modifier.height(16.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        // INCOMING
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.ArrowUpward,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Column {

                                Text(
                                    "Incoming",
                                    fontSize = 12.sp
                                )

                                Text(
                                    "$currency${"%.2f".format(income)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        // OUTGOING
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {

                            Icon(
                                Icons.Default.ArrowDownward,
                                contentDescription = null,
                                tint = Color(0xFFC62828)
                            )

                            Spacer(
                                modifier = Modifier.width(4.dp)
                            )

                            Column {

                                Text(
                                    "Outgoing",
                                    fontSize = 12.sp
                                )

                                Text(
                                    "$currency${"%.2f".format(expense)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(
                modifier = Modifier.height(24.dp)
            )

            // ─────────────────────────────────────
            // TRANSACTIONS TITLE
            // ─────────────────────────────────────

            Text(
                "Transactions",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier = Modifier.height(8.dp)
            )

            // ─────────────────────────────────────
            // TRANSACTION LIST
            // ─────────────────────────────────────

            LazyColumn {

                items(transactions) { txn ->

                    TransactionRow(
                        txn = txn,
                        currency = currency,

                        // Tap opens the transaction profile
                        // with this counterparty
                        onClick = {
                            onTransactionClick(
                                txn.merchant ?: txn.bankName
                            )
                        },

                        // IMPORTANT:
                        // Delete dialog opens ONLY after long press
                        onLongPress = {
                            txnToDelete = txn
                        }
                    )

                    HorizontalDivider()
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // EDIT BALANCE DIALOG
    // ─────────────────────────────────────────────

    if (showBalanceDialog) {

        var input by remember {
            mutableStateOf(balance.toString())
        }

        AlertDialog(

            onDismissRequest = {
                showBalanceDialog = false
            },

            title = {
                Text("Set Your Balance")
            },

            text = {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    OutlinedTextField(
                        value = input,

                        onValueChange = {
                            input = it
                        },

                        label = {
                            Text("Balance")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        "⚠️ This sets your starting balance. Future transactions " +
                                "(income & expenses) will be added or deducted from this amount automatically.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {

                        input.toDoubleOrNull()?.let {
                            viewModel.updateBalance(it)
                        }

                        showBalanceDialog = false
                    }
                ) {

                    Text("Save")
                }
            },

            dismissButton = {

                OutlinedButton(
                    onClick = {
                        showBalanceDialog = false
                    }
                ) {

                    Text("Cancel")
                }
            }
        )
    }

    // ─────────────────────────────────────────────
    // DELETE CONFIRMATION DIALOG
    // ─────────────────────────────────────────────

    txnToDelete?.let { txn ->

        AlertDialog(

            onDismissRequest = {
                txnToDelete = null
            },

            icon = {

                Icon(
                    Icons.Default.ArrowDownward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },

            title = {
                Text("Delete Transaction")
            },

            text = {

                Text(
                    "Do you want to delete this transaction? " +
                            "You can't retrieve it once deleted."
                )
            },

            confirmButton = {

                Button(

                    onClick = {

                        viewModel.deleteTransaction(txn)

                        txnToDelete = null
                    },

                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {

                    Text("Delete")
                }
            },

            dismissButton = {

                OutlinedButton(

                    onClick = {
                        txnToDelete = null
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }

    // ─────────────────────────────────────────────
    // ADD TRANSACTION DIALOG
    // ─────────────────────────────────────────────

    if (showAddDialog) {

        var amount by remember {
            mutableStateOf("")
        }

        var merchant by remember {
            mutableStateOf("")
        }

        var bankName by remember {
            mutableStateOf("")
        }

        var isExpense by remember {
            mutableStateOf(true)
        }

        AlertDialog(

            onDismissRequest = {
                showAddDialog = false
            },

            title = {
                Text("Add Transaction")
            },

            text = {

                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    // AMOUNT
                    OutlinedTextField(

                        value = amount,

                        onValueChange = {
                            amount = it
                        },

                        label = {
                            Text("Amount")
                        },

                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),

                        modifier = Modifier.fillMaxWidth()
                    )

                    // MERCHANT
                    OutlinedTextField(

                        value = merchant,

                        onValueChange = {
                            merchant = it
                        },

                        label = {
                            Text("Merchant (optional)")
                        },

                        modifier = Modifier.fillMaxWidth()
                    )

                    // BANK
                    OutlinedTextField(

                        value = bankName,

                        onValueChange = {
                            bankName = it
                        },

                        label = {
                            Text("Bank / Source")
                        },

                        modifier = Modifier.fillMaxWidth()
                    )

                    // TYPE
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Text(
                            "Type: ",
                            fontWeight = FontWeight.Medium
                        )

                        FilterChip(

                            selected = isExpense,

                            onClick = {
                                isExpense = true
                            },

                            label = {
                                Text("Expense")
                            },

                            modifier = Modifier.padding(
                                end = 8.dp
                            )
                        )

                        FilterChip(

                            selected = !isExpense,

                            onClick = {
                                isExpense = false
                            },

                            label = {
                                Text("Income")
                            }
                        )
                    }
                }
            },

            confirmButton = {

                Button(

                    onClick = {

                        val parsed =
                            amount.toBigDecimalOrNull()

                        if (
                            parsed != null &&
                            parsed > BigDecimal.ZERO
                        ) {

                            viewModel.addTransaction(

                                amount = parsed,

                                type = if (isExpense)
                                    "EXPENSE"
                                else
                                    "INCOME",

                                merchant = merchant,

                                bankName = bankName
                            )

                            showAddDialog = false
                        }
                    }
                ) {

                    Text("Add")
                }
            },

            dismissButton = {

                OutlinedButton(

                    onClick = {
                        showAddDialog = false
                    }

                ) {

                    Text("Cancel")
                }
            }
        )
    }
}


// ═════════════════════════════════════════════════════
// TRANSACTION ROW
// ═════════════════════════════════════════════════════

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TransactionRow(
    txn: TransactionEntity,
    currency: String,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {

    val isIncome = txn.type == "INCOME"

    val dateStr = remember(txn.timestamp) {

        SimpleDateFormat(
            "dd MMM, hh:mm a",
            Locale.getDefault()
        ).format(
            Date(txn.timestamp)
        )
    }

    Row(

        modifier = Modifier

            .fillMaxWidth()

            // ───────────────────────────────
            // TAP → TRANSACTION PROFILE
            // LONG PRESS → DELETE
            // ───────────────────────────────
            .combinedClickable(

                onClick = onClick,

                onLongClick = {
                    onLongPress()
                }
            )

            .padding(
                vertical = 12.dp
            ),

        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {

            Text(
                txn.merchant ?: txn.bankName,
                fontWeight = FontWeight.Medium
            )

            Text(
                dateStr,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }

        Text(

            "${if (isIncome) "+" else "-"}" +
                    "$currency${"%.2f".format(txn.amount)}",

            color = if (isIncome)
                Color(0xFF2E7D32)
            else
                Color(0xFFC62828),

            fontWeight = FontWeight.Bold
        )
    }
}