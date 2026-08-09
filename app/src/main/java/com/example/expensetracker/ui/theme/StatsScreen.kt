package com.example.expensetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppViewModel
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.patrykandpatrick.vico.core.entry.entryOf

@Composable
fun StatsScreen(viewModel: AppViewModel) {
    val monthlyData = remember { viewModel.monthlySpendingLast6Months() }
    val currency by viewModel.currency.collectAsState()
    var merchantSpending by remember { mutableStateOf(listOf<Pair<String, Double>>()) }

    LaunchedEffect(Unit) { merchantSpending = viewModel.topMerchants() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Spending Overview", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))

        val entries = monthlyData.mapIndexed { index, (_, value) -> entryOf(index.toFloat(), value) }
        Chart(
            chart = columnChart(),
            model = entryModelOf(entries),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )

        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            monthlyData.forEach { (label, _) -> Text(label, fontSize = 12.sp) }
        }

        Spacer(Modifier.height(32.dp))
        Text("Top Merchants", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        LazyColumn {
            items(merchantSpending) { (merchant, total) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(merchant)
                    Text("$currency${"%.2f".format(total)}", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}