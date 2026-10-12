package com.example.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import java.math.BigDecimal

@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onTransactionClick: (String) -> Unit   // still wired for deep-links if needed
) {
    val balance by viewModel.balance.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val expense by viewModel.monthlyExpense.collectAsState()
    val currency by viewModel.currency.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showBalanceDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
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
                    Text("Available Balance", fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "$currency${"%.2f".format(balance)}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showBalanceDialog = true }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {

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
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Incoming", fontSize = 12.sp)
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Column {
                                Text("Outgoing", fontSize = 12.sp)
                                Text(
                                    "$currency${"%.2f".format(expense)}",
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // EDIT BALANCE DIALOG
    // ─────────────────────────────────────────────

    if (showBalanceDialog) {
        var input by remember { mutableStateOf(balance.toString()) }

        AlertDialog(
            onDismissRequest = { showBalanceDialog = false },
            title = { Text("Set Your Balance") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = { input = it },
                        label = { Text("Balance") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                Button(onClick = {
                    input.toDoubleOrNull()?.let { viewModel.updateBalance(it) }
                    showBalanceDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showBalanceDialog = false }) { Text("Cancel") }
            }
        )
    }

    // ─────────────────────────────────────────────
    // ADD TRANSACTION DIALOG
    // ─────────────────────────────────────────────

    if (showAddDialog) {
        var amount by remember { mutableStateOf("") }
        var merchant by remember { mutableStateOf("") }
        var bankName by remember { mutableStateOf("") }
        var isExpense by remember { mutableStateOf(true) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = merchant,
                        onValueChange = { merchant = it },
                        label = { Text("Merchant (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = { Text("Bank / Source") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Type: ", fontWeight = FontWeight.Medium)
                        FilterChip(
                            selected = isExpense,
                            onClick = { isExpense = true },
                            label = { Text("Expense") },
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        FilterChip(
                            selected = !isExpense,
                            onClick = { isExpense = false },
                            label = { Text("Income") }
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = amount.toBigDecimalOrNull()
                    if (parsed != null && parsed > BigDecimal.ZERO) {
                        viewModel.addTransaction(
                            amount = parsed,
                            type = if (isExpense) "EXPENSE" else "INCOME",
                            merchant = merchant,
                            bankName = bankName
                        )
                        showAddDialog = false
                    }
                }) { Text("Add") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) { Text("Cancel") }
            }
        )
    }
}
