package com.example.expensetracker.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "user_prefs")

private object PrefKeys {
    val BALANCE = doublePreferencesKey("balance")
    val NAME = stringPreferencesKey("name")
    val EMAIL = stringPreferencesKey("email")
    val CURRENCY = stringPreferencesKey("currency")
}

class UserPreferences(private val context: Context) {
    val balance: Flow<Double> = context.dataStore.data.map { it[PrefKeys.BALANCE] ?: 0.0 }
    val name: Flow<String> = context.dataStore.data.map { it[PrefKeys.NAME] ?: "" }
    val email: Flow<String> = context.dataStore.data.map { it[PrefKeys.EMAIL] ?: "" }
    val currency: Flow<String> = context.dataStore.data.map { it[PrefKeys.CURRENCY] ?: "₹" }

    suspend fun setBalance(value: Double) { context.dataStore.edit { it[PrefKeys.BALANCE] = value } }
    suspend fun setName(value: String) { context.dataStore.edit { it[PrefKeys.NAME] = value } }
    suspend fun setEmail(value: String) { context.dataStore.edit { it[PrefKeys.EMAIL] = value } }
    suspend fun setCurrency(value: String) { context.dataStore.edit { it[PrefKeys.CURRENCY] = value } }
}