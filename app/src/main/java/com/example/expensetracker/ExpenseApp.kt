package com.example.expensetracker

import android.app.Application
import androidx.room.Room
import com.example.expensetracker.data.AppDatabase

class ExpenseApp : Application() {
    lateinit var db: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        db = Room.databaseBuilder(this, AppDatabase::class.java, "expense-db").build()
    }
}