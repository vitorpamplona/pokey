package com.koalasat.pokey.models

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object PrefKeys {
    const val NOSTR_PUBKEY = "nostr_pubkey"
}

object EncryptedStorage {
    private const val PREFERENCES_NAME = "secret_keeper"

    private lateinit var sharedPreferences: SharedPreferences

    private val _pubKey = MutableLiveData<String>()
    val pubKey: LiveData<String> get() = _pubKey

    fun init(context: Context) {
        val masterKey: MasterKey =
            MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        sharedPreferences = EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ) as EncryptedSharedPreferences

        _pubKey.value = sharedPreferences.getString(PrefKeys.NOSTR_PUBKEY, "").toString()
    }

    fun updatePubKey(newValue: String) {
        sharedPreferences.edit().putString(PrefKeys.NOSTR_PUBKEY, newValue).apply()
        _pubKey.value = newValue
    }
}
