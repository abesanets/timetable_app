package com.example.schedule.features.update

import com.example.schedule.features.update.data.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateManagerTest {

    @Test
    fun testIsNewerVersion() {
        // Обычные мажорные и минорные версии
        assertTrue(UpdateManager.isNewerVersion("4.5", "v4.6"))
        assertTrue(UpdateManager.isNewerVersion("4.5", "5.0"))
        assertTrue(UpdateManager.isNewerVersion("4.5", "4.5.1"))
        assertTrue(UpdateManager.isNewerVersion("v4.5", "v4.5.2"))

        // Одинаковые версии
        assertFalse(UpdateManager.isNewerVersion("4.5", "4.5"))
        assertFalse(UpdateManager.isNewerVersion("4.5", "v4.5"))
        assertFalse(UpdateManager.isNewerVersion("v4.5", "4.5"))

        // Старые версии на сервере
        assertFalse(UpdateManager.isNewerVersion("4.5", "4.4"))
        assertFalse(UpdateManager.isNewerVersion("4.5", "3.9"))
        assertFalse(UpdateManager.isNewerVersion("4.5.1", "4.5"))
    }
}
