package com.example.expensetracker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(viewModel: AppViewModel) {
    val balance by viewModel.balance.collectAsState()
    val income by viewModel.monthlyIncome.collectAsState()
    val expense by viewModel.monthlyExpense.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val currency by viewModel.currency.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                Text("Available Balance", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "$currency${"%.2f".format(balance)}",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showEditDialog = true }
                )
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = Color(0xFF2E7D32))
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("Incoming", fontSize = 12.sp)
                            Text("$currency${"%.2f".format(income)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = Color(0xFFC62828))
                        Spacer(Modifier.width(4.dp))
                        Column {
                            Text("Outgoing", fontSize = 12.sp)
                            Text("$currency${"%.2f".format(expense)}", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Transactions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(transactions) { txn ->
                TransactionRow(txn, currency)
                Divider()
            }
        }
    }

    if (showEditDialog) {
        var input by remember { mutableStateOf(balance.toString()) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Balance") },
            text = { OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Balance") }) },
            confirmButton = {
                TextButton(onClick = {
                    input.toDoubleOrNull()?.let { viewModel.updateBalance(it) }
                    showEditDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun TransactionRow(txn: TransactionEntity, currency: String) {
    val isIncome = txn.type == "INCOME"
    val dateStr = remember(txn.timestamp) {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(txn.timestamp))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(txn.merchant ?: txn.bankName, fontWeight = FontWeight.Medium)
            Text(dateStr, fontSize = 12.sp, color = Color.Gray)
        }
        Text(
            "${if (isIncome) "+" else "-"}$currency${"%.2f".format(txn.amount)}",
            color = if (isIncome) Color(0xFF2E7D32) else Color(0xFFC62828),
            fontWeight = FontWeight.Bold
        )
    }
}