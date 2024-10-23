package com.koalasat.pokey.models

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.koalasat.pokey.Pokey

object PrefKeys {
    const val NOSTR_PUBKEY = "nostr_pubkey"
    const val NOSTR_RELAYS = "nostr_relays"
}

object EncryptedStorage {
    private const val PREFERENCES_NAME = "secret_keeper"

    fun preferences(): EncryptedSharedPreferences {
        val context = Pokey.getInstance()
        val masterKey: MasterKey =
            MasterKey.Builder(context, MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

        return EncryptedSharedPreferences.create(
            context,
            PREFERENCES_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        ) as EncryptedSharedPreferences
    }
}
