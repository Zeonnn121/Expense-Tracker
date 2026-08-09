package com.example.expensetracker

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.ritesh.parser.core.bank.BankParserFactory
import com.example.expensetracker.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TxnNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        android.util.Log.d("NotifDebug", "Listener CONNECTED")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val extras = sbn.notification.extras
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""

        android.util.Log.d("NotifDebug", "title=$title pkg=${sbn.packageName} text=$text")

        if (text == null) return

        // Try matching by notification title first (e.g. "HDFC Bank"),
        // since canHandle() was written for SMS sender IDs like "HDFCBK"
        val parser = BankParserFactory.getParser(title)
            ?: BankParserFactory.getParser(sbn.packageName)
            ?: return
        android.util.Log.d("NotifDebug", "Parser matched: ${parser.getBankName()}")
        val parsed = parser.parse(text, title, System.currentTimeMillis()) ?: return
        android.util.Log.d("NotifDebug", "Parsed amount=${parsed.amount} type=${parsed.type} merchant=${parsed.merchant}")
        val entity = TransactionEntity(
            amount = parsed.amount,
            type = parsed.type.name,
            merchant = parsed.merchant,
            bankName = parsed.bankName,
            timestamp = parsed.timestamp
        )

        val app = applicationContext as ExpenseApp
        CoroutineScope(Dispatchers.IO).launch {
            app.db.transactionDao().insert(entity)
        }
    }
}