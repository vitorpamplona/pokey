package com.koalasat.pokey.ui.relays

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koalasat.pokey.Pokey
import com.koalasat.pokey.R

class RelaysViewModel() : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = Pokey.getInstance().getString(R.string.not_started)
    }
    val text: LiveData<String> = _text
}