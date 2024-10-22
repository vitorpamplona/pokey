package com.koalasat.pokey.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.koalasat.pokey.R
import com.koalasat.pokey.models.EncryptedStorage.preferences
import com.koalasat.pokey.models.PrefKeys
import com.vitorpamplona.ammolite.relays.COMMON_FEED_TYPES
import com.vitorpamplona.ammolite.relays.Client
import com.vitorpamplona.ammolite.relays.Relay
import com.vitorpamplona.ammolite.relays.RelayPool
import com.vitorpamplona.ammolite.relays.TypedFilter
import com.vitorpamplona.ammolite.relays.filters.EOSETime
import com.vitorpamplona.ammolite.relays.filters.SincePerRelayFilter
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.Nip19Bech32.uriToRoute
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.EventInterface
import java.time.Instant
import java.util.Timer
import java.util.TimerTask

class NotificationsService : Service() {
    private var channelRelaysId = "RelaysConnections"
    private var channelNotificationsId = "Notifications"
    private var lastNotificationAt: EOSETime = EOSETime(Instant.now().toEpochMilli() / 1000)

    private val timer = Timer()

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

    override fun onBind(intent: Intent): IBinder {
        return null!!
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("Pokey", "Starting foreground service...")
        isRunning = true
        startForeground(1, createNotification())
        startSubscription()
        keepAlive()

        return START_STICKY
    }

    override fun onDestroy() {
        timer.cancel()
        isRunning = false
        super.onDestroy()
        RelayPool.disconnect()
    }

    private fun startSubscription() {
        val hexKey = getHexKey()
        if (hexKey.isEmpty()) return

        if (!Client.isSubscribed(clientListener)) Client.subscribe(clientListener)

        Client.sendFilter("pushNotifications", listOf(TypedFilter(
            types = COMMON_FEED_TYPES,
            filter = SincePerRelayFilter(
                kinds = listOf(1),
                tags = mapOf("p" to listOf(hexKey)),
                since = RelayPool.getAll().associate { it.url to lastNotificationAt }
            ),
        )))
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
        if (!event.hasVerifiedSignature()) return

        val pubKey = preferences().getString(PrefKeys.NOSTR_PUBKEY, "").toString()
        if (event.pubKey == pubKey) return

        var title = getString(R.string.unknown)
        var text = ""

        if (event.kind == 1) {
            title = if (event.content().contains("nostr:${pubKey}"))
                getString(R.string.new_mention)
            else
                getString(R.string.new_reply)
            text = event.content()
        }

        if (text.isNotEmpty()) lastNotificationAt = EOSETime(event.createdAt)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
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

    companion object {
        var isRunning = false
    }
}
