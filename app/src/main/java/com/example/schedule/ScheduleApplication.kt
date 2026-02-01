package com.example.schedule

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class ScheduleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize Yandex MapKit with the API key
        MapKitFactory.setLocale("ru_RU")
        MapKitFactory.setApiKey("e206298f-f5e1-487a-a32f-1d4c09bd879f")
        MapKitFactory.initialize(this)
    }
}