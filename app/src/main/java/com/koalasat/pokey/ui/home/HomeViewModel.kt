package com.koalasat.pokey.ui.home

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koalasat.pokey.Pokey
import com.koalasat.pokey.models.EncryptedStorage
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.Nip19Bech32.uriToRoute

class HomeViewModel : ViewModel() {
    private val _npubInput = MutableLiveData<String>()
    val npubInput: LiveData<String> get() = _npubInput

    private val _serviceStart = MutableLiveData<Boolean>()
    val serviceStart: LiveData<Boolean> get() = _serviceStart

    private val _validationResult = MutableLiveData<Boolean>()
    val validationResult: LiveData<Boolean> get() = _validationResult

    init {
        _npubInput.value = EncryptedStorage.pubKey.value
        _serviceStart.value = Pokey.isEnabled.value
        EncryptedStorage.pubKey.observeForever { text ->
            _npubInput.value = text
        }
    }

    fun updateServiceStart(value: Boolean) {
        if (value) {
            Pokey.getInstance().startService()
        } else {
            Pokey.getInstance().stopService()
        }
        _serviceStart.value = value
    }

    fun updateNpubInput(text: String) {
        _npubInput.value = text
        validateNpubInput()
        Log.e("Pokey", "validation: " + _validationResult.value.toString())
        if (_validationResult.value == true) {
            EncryptedStorage.updatePubKey(text)
        }
    }

    private fun validateNpubInput() {
        val parseReturn = uriToRoute(_npubInput.value)

        when (parseReturn?.entity) {
            is Nip19Bech32.NPub -> {
                _validationResult.value = true
                return
            }
        }

        _validationResult.value = false
    }
}
