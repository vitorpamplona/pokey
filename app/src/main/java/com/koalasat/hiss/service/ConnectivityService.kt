package com.koalasat.hiss.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.koalasat.hiss.Hiss
import com.koalasat.hiss.R
import com.vitorpamplona.ammolite.relays.*
import com.vitorpamplona.ammolite.relays.filters.SincePerRelayFilter
import com.vitorpamplona.quartz.encoders.Hex
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.toNpub
import com.vitorpamplona.quartz.events.Event
import com.vitorpamplona.quartz.events.EventInterface
import java.util.Timer
import java.util.TimerTask

class ConnectivityService : Service() {
    private var channelRelaysId = "RelaysConnections"
    private var channelNotificationsId = "Notifications"

    private var isSubscribed = false

    private val timer = Timer()

    private val clientListener =
        object : Client.Listener {
            override fun onAuth(relay: Relay, challenge: String) {
                Log.d("Hiss", "Relay on Auth: ${relay.url}")
            }

            override fun onSend(relay: Relay, msg: String, success: Boolean) {
                Log.d("Hiss", "Relay send: ${relay.url}")
            }

            override fun onBeforeSend(relay: Relay, event: EventInterface) {
                Log.d("Hiss", "Relay Before Send: ${relay.url} - $event")
            }

            override fun onError(error: Error, subscriptionId: String, relay: Relay) {
                Log.d("Hiss", "Relay Error: ${relay.url} - ${error.message}")
            }

            override fun onEvent(
                event: Event,
                subscriptionId: String,
                relay: Relay,
                afterEOSE: Boolean,
            ) {
                Log.d("Hiss", "Relay Event: ${relay.url} - ${event.id}")
                displayNoteNotification(event)
            }

            override fun onNotify(relay: Relay, description: String) {
                Log.d("Hiss", "Relay On Notify: ${relay.url} - $description")
            }

            override fun onRelayStateChange(type: Relay.StateType, relay: Relay, subscriptionId: String?) {
                Log.d("Hiss", "Relay state change: ${relay.url} - $type")
            }

            override fun onSendResponse(
                eventId: String,
                success: Boolean,
                message: String,
                relay: Relay
            ) {
                Log.d("Hiss", "Relay send response: ${relay.url} - $eventId")
            }
        }

    override fun onBind(intent: Intent): IBinder {
        return null!!
    }

    override fun onCreate() {
        if (Hiss.getInstance().isStarted) return

        Log.d("Hiss", "Starting foreground service...")
        startForeground(1, createNotification())
        startSubscription()
        keepAlive()

        super.onCreate()
    }

    override fun onDestroy() {
        Hiss.getInstance().isStarted = false
        isSubscribed = false
        timer.cancel()
        super.onDestroy()
        RelayPool.disconnect()
    }

    private fun startSubscription() {
        val instance = Hiss.getInstance()
        if (isSubscribed || instance.hexPub.isEmpty() || !instance.isStarted) return

        isSubscribed = true

        if (!Client.isSubscribed(clientListener)) Client.subscribe(clientListener)

        Client.sendFilter("pushNotifications", listOf(TypedFilter(
            types = COMMON_FEED_TYPES,
            filter = SincePerRelayFilter(
                kinds = listOf(1),
                tags = mapOf("p" to listOf(instance.hexPub)),
                limit = 1,
            ),
        )))
    }

    private fun closeSubscription() {
        val instance = Hiss.getInstance()
        if (!isSubscribed || instance.isStarted) return

        isSubscribed = false

        Client.close("pushNotifications")
    }

    private fun keepAlive() {
        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    if (isSubscribed) {
                        closeSubscription()
                    } else {
                        startSubscription()
                    }
                }
            },
            5000,
            2000,
        )

        timer.schedule(
            object : TimerTask() {
                override fun run() {
                    RelayPool.getAll().forEach {
                        if (!it.isConnected()) {
                            Log.d(
                                "Hiss",
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
        Log.d("Hiss", "Building channels...")
        val channelRelays = NotificationChannel(channelRelaysId, getString(R.string.relays_connection), NotificationManager.IMPORTANCE_DEFAULT)
        channelRelays.setSound(null, null)
        val channelNotification = NotificationChannel(channelNotificationsId, getString(R.string.notifications), NotificationManager.IMPORTANCE_HIGH)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channelRelays)
        notificationManager.createNotificationChannel(channelNotification)

        Log.d("Hiss", "Building notification...")
        val notificationBuilder =
            NotificationCompat.Builder(this, channelRelaysId)
                .setContentTitle(getString(R.string.hiss_is_running_in_background))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setSmallIcon(R.drawable.ic_launcher_foreground)

        return notificationBuilder.build()
    }

    private fun displayNoteNotification(event: Event) {
        if (!event.hasVerifiedSignature()) return
        val instance = Hiss.getInstance()

        var title = getString(R.string.unknown)
        var text = ""

        if (event.kind == 1) {
            val nPub = Hex.decode(instance.hexPub).toNpub()
            title = if (event.content().contains("nostr:${nPub}"))
                getString(R.string.new_mention)
            else
                getString(R.string.new_reply)
            text = event.content()
        }

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
}
