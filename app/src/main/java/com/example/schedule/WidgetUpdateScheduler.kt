package com.example.schedule

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object WidgetUpdateScheduler {
    private const val WORK_NAME = "periodic_widget_update"
    
    fun scheduleUpdate(context: Context, intervalMinutes: Long) {
        val workManager = WorkManager.getInstance(context)
        
        if (intervalMinutes <= 0) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        // Minimum interval for WorkManager is 15 minutes
        val safeInterval = Math.max(intervalMinutes, 15)
            
        val updateRequest = PeriodicWorkRequestBuilder<WidgetUpdateWorker>(
            safeInterval, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
            
        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            updateRequest
        )
    }
    
    fun cancelUpdate(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
