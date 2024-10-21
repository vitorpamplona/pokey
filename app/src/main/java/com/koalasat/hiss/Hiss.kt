package com.koalasat.hiss

import android.app.Application
import android.content.Intent
import com.koalasat.hiss.service.ConnectivityService
import com.vitorpamplona.ammolite.relays.COMMON_FEED_TYPES
import com.vitorpamplona.ammolite.relays.Client
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class Hiss : Application() {
    val applicationIOScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    var isStarted: Boolean = false
    var hexPub: String = ""

    override fun onCreate() {
        super.onCreate()

        RelayPool.register(Client)
        RelayPool.addRelay(
            Relay(
                "wss://nostr.satstralia.com",
                read = true,
                write = false,
                forceProxy = false,
                activeTypes = COMMON_FEED_TYPES
            ),
        )

        this.startForegroundService(
            Intent(
                this,
                ConnectivityService::class.java,
            ),
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        applicationIOScope.cancel()
    }

    companion object {
        @Volatile
        private var instance: Hiss? = null

        fun getInstance(): Hiss =
            instance ?: synchronized(this) {
                instance ?: Hiss().also { instance = it }
            }
    }
}