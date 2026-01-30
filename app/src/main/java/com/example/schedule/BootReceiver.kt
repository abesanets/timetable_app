package com.example.schedule

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Receiver для перезапуска уведомлений после перезагрузки устройства
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            
            // Перезапускаем уведомления, если они были включены
            CoroutineScope(Dispatchers.IO).launch {
                val preferencesManager = PreferencesManager(context)
                val notificationsEnabled = preferencesManager.notificationsEnabled.first()
                
                if (notificationsEnabled) {
                    val scheduler = NotificationScheduler(context)
                    scheduler.scheduleNotification()
                }
            }
        }
    }
}
