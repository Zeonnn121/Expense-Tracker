package com.example.expensetracker.ui

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.AppViewModel

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val savedName by viewModel.userName.collectAsState()
    val savedEmail by viewModel.userEmail.collectAsState()
    val savedCurrency by viewModel.currency.collectAsState()

    var name by remember(savedName) { mutableStateOf(savedName) }
    var email by remember(savedEmail) { mutableStateOf(savedEmail) }
    var currency by remember(savedCurrency) { mutableStateOf(savedCurrency) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profile", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))

        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email (optional)") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(value = currency, onValueChange = { currency = it }, label = { Text("Currency symbol") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.updateProfile(name, email, currency) }, modifier = Modifier.fillMaxWidth()) { Text("Save") }

        Spacer(Modifier.height(32.dp))
        Divider()
        Spacer(Modifier.height(16.dp))
        Text("Permissions", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Grant Notification Access") }
    }
}