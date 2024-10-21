package com.koalasat.hiss.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koalasat.hiss.Hiss
import com.vitorpamplona.quartz.encoders.Nip19Bech32
import com.vitorpamplona.quartz.encoders.Nip19Bech32.uriToRoute

class HomeViewModel : ViewModel() {
    private val _npubInput = MutableLiveData<String>()
    val npubInput: LiveData<String> get() = _npubInput

    private val _validationResult = MutableLiveData<Boolean>()
    val validationResult: LiveData<Boolean> get() = _validationResult

    fun setNpubInput(text: String) {
        _npubInput.value = text
        validateNpubInput()
    }

    private fun validateNpubInput() {
        val parseReturn = uriToRoute(_npubInput.value)

        when (val parsed = parseReturn?.entity) {
            is Nip19Bech32.NPub -> {
                _validationResult.value = true
                Hiss.getInstance().hexPub = parsed.hex
            }
        }
    }
}
