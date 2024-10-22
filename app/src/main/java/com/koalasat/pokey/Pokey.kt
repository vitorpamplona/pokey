package com.koalasat.pokey

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import com.koalasat.pokey.service.NotificationsService
import com.vitorpamplona.ammolite.relays.COMMON_FEED_TYPES
import com.vitorpamplona.ammolite.relays.Client
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class Pokey : Application() {
    private val applicationIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        instance = this

        RelayPool.register(Client)

        this.startForegroundService(
            Intent(
                this,
                NotificationsService::class.java,
            ),
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationIOScope.cancel()
    }

    fun startService() {
        val intent = Intent(applicationContext, NotificationsService::class.java)

        RelayPool.addRelay(
            Relay(
                "wss://nostr.satstralia.com",
                read = true,
                write = false,
                forceProxy = false,
                activeTypes = COMMON_FEED_TYPES
            ),
        )
        applicationContext.startService(intent)
    }

    fun stopService() {
        val intent = Intent(applicationContext, NotificationsService::class.java)
        applicationContext.stopService(intent)
        RelayPool.getAll().forEach {
            it.disconnect()
            RelayPool.removeRelay(it)
        }
    }

    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val runningServices = activityManager.getRunningServices(Int.MAX_VALUE)

        for (serviceInfo in runningServices) {
            if (serviceInfo.service.className == serviceClass.name) {
                return true
            }
        }
        return false
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