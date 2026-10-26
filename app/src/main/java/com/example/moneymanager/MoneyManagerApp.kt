package com.example.moneymanager

import android.app.Application
import com.example.moneymanager.worker.RecurringTransactionWorker
import com.example.moneymanager.worker.SyncWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MoneyManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        RecurringTransactionWorker.enqueuePeriodicWork(this)
        SyncWorker.enqueuePeriodicSync(this)
        com.example.moneymanager.widget.MoneyManagerWidgetProvider.updateAllWidgets(this)
    }
}