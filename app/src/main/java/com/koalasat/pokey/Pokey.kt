package com.koalasat.pokey

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import com.koalasat.pokey.database.AppDatabase
import com.koalasat.pokey.models.EncryptedStorage.preferences
import com.koalasat.pokey.models.PrefKeys
import com.koalasat.pokey.service.NotificationsService
import com.vitorpamplona.ammolite.relays.COMMON_FEED_TYPES
import com.vitorpamplona.ammolite.relays.Client
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.ConcurrentHashMap

class Pokey : Application() {
    private val applicationIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        RelayPool.register(Client)
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationIOScope.cancel()
    }

    fun startService() {
        this.startForegroundService(
            Intent(
                this,
                NotificationsService::class.java,
            ),
        )
        NotificationsService.setRunningState(true)
    }

    fun stopService() {
        val intent = Intent(applicationContext, NotificationsService::class.java)
        applicationContext.stopService(intent)
        NotificationsService.setRunningState(false)
    }

    companion object {
        @Volatile
        private var instance: Pokey? = null

        fun getInstance(): Pokey =
            instance ?: synchronized(this) {
                instance ?: Pokey().also { instance = it }
            }
    }
}