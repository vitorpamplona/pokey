package com.koalasat.pokey

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
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

        updateIsEnabled(isForegroundServiceEnabled(this))

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
        saveForegroundServicePreference(this, true)
    }

    fun stopService() {
        val intent = Intent(applicationContext, NotificationsService::class.java)
        applicationContext.stopService(intent)
        saveForegroundServicePreference(this, false)
    }

    companion object {
        private val _isEnabled = MutableLiveData(false)
        val isEnabled: LiveData<Boolean> get() = _isEnabled

        @Volatile
        private var instance: Pokey? = null

        fun getInstance(): Pokey =
            instance ?: synchronized(this) {
                instance ?: Pokey().also { instance = it }
            }

        fun updateIsEnabled(value: Boolean) {
            _isEnabled.value = value
        }

        private fun saveForegroundServicePreference(context: Context, value: Boolean) {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences("PokeyPreferences", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putBoolean("foreground_service_enabled", value)
            editor.apply()
            updateIsEnabled(value)
        }

        fun isForegroundServiceEnabled(context: Context): Boolean {
            val sharedPreferences: SharedPreferences = context.getSharedPreferences("PokeyPreferences", Context.MODE_PRIVATE)
            return sharedPreferences.getBoolean("foreground_service_enabled", false)
        }
    }
}