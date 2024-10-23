package com.koalasat.pokey.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.koalasat.pokey.Connectivity
import com.koalasat.pokey.Pokey
import com.koalasat.pokey.R
import com.koalasat.pokey.database.AppDatabase
import com.koalasat.pokey.database.NotificationEntity
import com.koalasat.pokey.models.EncryptedStorage.preferences
import com.koalasat.pokey.models.PrefKeys
import com.vitorpamplona.ammolite.relays.COMMON_FEED_TYPES
import com.vitorpamplona.ammolite.relays.Client
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool
import com.vitorpamplona.ammolite.relays.TypedFilter
import com.vitorpamplona.ammolite.relays.filters.EOSETime
import com.vitorpamplona.ammolite.relays.filters.SincePerRelayFilter
import com.vitorpamplona.ammolite.service.HttpClientManager
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.Nip19Bech32.uriToRoute
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.EventInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

class NotificationsService : Service() {
    private var channelRelaysId = "RelaysConnections"
    private var channelNotificationsId = "Notifications"

    private var defaultRelayUrls = arrayOf(
        "wss://relay.damus.io", "wss://offchain.pub", "wss://relay.snort.social", "wss://nos.lol", "wss://nostr.wine"
    )

    private val timer = Timer()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val clientListener =
        object : Client.Listener {
            override fun onAuth(relay: Relay, challenge: String) {
                Log.d("Pokey", "Relay on Auth: ${relay.url}")
            }

            override fun onSend(relay: Relay, msg: String, success: Boolean) {
                Log.d("Pokey", "Relay send: ${relay.url}")
            }

            override fun onBeforeSend(relay: Relay, event: EventInterface) {
                Log.d("Pokey", "Relay Before Send: ${relay.url} - $event")
            }

            override fun onError(error: Error, subscriptionId: String, relay: Relay) {
                Log.d("Pokey", "Relay Error: ${relay.url} - ${error.message}")
            }

            override fun onEvent(
                event: Event,
                subscriptionId: String,
                relay: Relay,
                afterEOSE: Boolean,
            ) {
                Log.d("Pokey", "Relay Event: ${relay.url} - ${event.id}")

                displayNoteNotification(event)
            }

            override fun onNotify(relay: Relay, description: String) {
                Log.d("Pokey", "Relay On Notify: ${relay.url} - $description")
            }

            override fun onRelayStateChange(type: Relay.StateType, relay: Relay, subscriptionId: String?) {
                Log.d("Pokey", "Relay state change: ${relay.url} - $type")
            }

            override fun onSendResponse(
                eventId: String,
                success: Boolean,
                message: String,
                relay: Relay
            ) {
                Log.d("Pokey", "Relay send response: ${relay.url} - $eventId")
            }
        }

    private val networkCallback =
        object : ConnectivityManager.NetworkCallback() {
            var lastNetwork: Network? = null

            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                if (lastNetwork != null && lastNetwork != network) {
                    scope.launch(Dispatchers.IO) {
                        stopSubscription()
                        delay(1000)
                        startSubscription()
                    }
                }

                lastNetwork = network
            }

            // Network capabilities have changed for the network
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                scope.launch(Dispatchers.IO) {
                    Log.d(
                        "ServiceManager NetworkCallback",
                        "onCapabilitiesChanged: ${network.networkHandle} hasMobileData ${Connectivity.isOnMobileData} hasWifi ${Connectivity.isOnWifiData}",
                    )
                    if (Connectivity.updateNetworkCapabilities(networkCapabilities)) {
                        stopSubscription()
                        delay(1000)
                        startSubscription()
                    }
                }
            }
        }

    override fun onBind(intent: Intent): IBinder {
        return null!!
    }

    override fun onCreate() {
        val connectivityManager =
            (getSystemService(ConnectivityManager::class.java) as ConnectivityManager)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Pokey", "Starting foreground service...")
        startForeground(1, createNotification())
        startSubscription()
        keepAlive()

        val connectivityManager =
            (getSystemService(ConnectivityManager::class.java) as ConnectivityManager)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        return START_STICKY
    }

    override fun onDestroy() {
        timer.cancel()
        stopSubscription()

        try {
            val connectivityManager =
                (getSystemService(ConnectivityManager::class.java) as ConnectivityManager)
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.d("connectivityManager", "Failed to unregisterNetworkCallback", e)
        }

        super.onDestroy()
    }

    private fun startSubscription() {
        val hexKey = getHexKey()
        if (hexKey.isEmpty()) return

        if (!Client.isSubscribed(clientListener)) Client.subscribe(clientListener)

        defaultRelayUrls.forEach {
            if (RelayPool.getRelays(it).isEmpty()) {
                RelayPool.addRelay(
                    Relay(
                        it,
                        read = true,
                        write = false,
                        forceProxy = false,
                        activeTypes = COMMON_FEED_TYPES
                    ),
                )
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(this@NotificationsService, getHexKey()).applicationDao()
            var latestNotification = dao.getLatestNotification()
            if (latestNotification == null) latestNotification = Instant.now().toEpochMilli() / 1000

            Client.sendFilter("pushNotifications", listOf(TypedFilter(
                types = COMMON_FEED_TYPES,
                filter = SincePerRelayFilter(
                    kinds = listOf(1),
                    tags = mapOf("p" to listOf(hexKey)),
                    since = RelayPool.getAll().associate { it.url to EOSETime(latestNotification) }
                ),
            )))
        }
    }

    private fun stopSubscription() {
        Client.unsubscribe(clientListener)
        RelayPool.disconnect()
    }

    private fun keepAlive() {
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    RelayPool.getAll().forEach {
                        if (!it.isConnected()) {
                            Log.d(
                                "Pokey",
                                "Relay ${it.url} is not connected, reconnecting...",
                            )
                            it.connectAndSendFiltersIfDisconnected()
                        }
                    }
                }
            },
            5000,
            61000,
        )
    }

    private fun createNotification(): Notification {
        Log.d("Pokey", "Building channels...")
        val channelRelays = NotificationChannel(channelRelaysId, getString(R.string.relays_connection), NotificationManager.IMPORTANCE_DEFAULT)
        channelRelays.setSound(null, null)

        val channelNotification = NotificationChannel(channelNotificationsId, getString(R.string.notifications), NotificationManager.IMPORTANCE_HIGH)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.createNotificationChannel(channelRelays)
        notificationManager.createNotificationChannel(channelNotification)

        Log.d("Pokey", "Building notification...")
        val notificationBuilder =
            NotificationCompat.Builder(this, channelRelaysId)
                .setContentTitle(getString(R.string.pokey_is_running_in_background))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)

        return notificationBuilder.build()
    }

    private fun displayNoteNotification(event: Event) {
        val pubKey = getHexKey()
        if (event.pubKey == pubKey) return

        CoroutineScope(Dispatchers.IO).launch {
            val dao = AppDatabase.getDatabase(this@NotificationsService, getHexKey()).applicationDao()
            val existsEvent = dao.existsNotification(event.id)
            if (existsEvent > 0) return@launch

            if (!event.hasVerifiedSignature()) return@launch

            dao.insertNotification(NotificationEntity(0, event.id, event.createdAt))

            var title = getString(R.string.unknown)
            var text = ""

            if (event.kind == 1) {
                title = if (event.content().contains("nostr:${pubKey}"))
                    getString(R.string.new_mention)
                else if (event.content().contains("nostr:nevent1"))
                    getString(R.string.new_repost)
                else
                    getString(R.string.new_reply)
                text = event.content().replace(Regex("nostr:[a-zA-Z0-9]+"), "")
            }

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val builder: NotificationCompat.Builder =
                NotificationCompat.Builder(
                    applicationContext,
                    channelNotificationsId
                )
                    .setContentTitle(title)
                    .setContentText(text)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)

            notificationManager.notify(event.hashCode(), builder.build())
        }
    }

    private fun getHexKey(): String {
        val pubKey = preferences().getString(PrefKeys.NOSTR_PUBKEY, "").toString()
        var hexKey = "";
        val parseReturn = uriToRoute(pubKey)
        when (val parsed = parseReturn?.entity) {
            is Nip19Bech32.NPub -> {
                hexKey = parsed.hex
            }
        }
        return hexKey
    }
}
