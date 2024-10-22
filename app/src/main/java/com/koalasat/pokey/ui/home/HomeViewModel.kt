package com.koalasat.pokey.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koalasat.pokey.Pokey
import com.koalasat.pokey.models.EncryptedStorage.preferences
import com.koalasat.pokey.models.PrefKeys
import com.koalasat.pokey.service.NotificationsService
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.Nip19Bech32.uriToRoute

class HomeViewModel : ViewModel() {
    private val _npubInput = MutableLiveData<String>().apply {
        value = preferences().getString(PrefKeys.NOSTR_PUBKEY, "").toString()
    }
    val npubInput: LiveData<String> get() = _npubInput

    private val _serviceStart = MutableLiveData<Boolean>().apply {
        value = NotificationsService.isRunning
    }
    val serviceStart: LiveData<Boolean> get() = _serviceStart

    private val _validationResult = MutableLiveData<Boolean>().apply {
        value = preferences().getString(PrefKeys.NOSTR_PUBKEY, "")?.isNotEmpty()
    }
    val validationResult: LiveData<Boolean> get() = _validationResult

    fun updateServiceStart(value: Boolean) {
        if (value) {
            Pokey.getInstance().stopService()
        } else {
            Pokey.getInstance().startService()
        }
        _serviceStart.value = value
    }

    fun updateNpubInput(text: String) {
        _npubInput.value = text
        validateNpubInput()
        if (_validationResult.value == true) {
            preferences().edit().apply {
                putString(PrefKeys.NOSTR_PUBKEY, text)
            }.apply()
        }
    }

    private fun validateNpubInput() {
        val parseReturn = uriToRoute(_npubInput.value)
        when (parseReturn?.entity) {
            is Nip19Bech32.NPub -> {
                _validationResult.value = true
            }
        }
    }
}
