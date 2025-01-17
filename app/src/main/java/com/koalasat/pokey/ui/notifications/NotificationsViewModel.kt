package com.koalasat.pokey.ui.notifications

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.koalasat.pokey.models.EncryptedStorage

class NotificationsViewModel : ViewModel() {

    private val _newReplies = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyReplies.value }
    val newReplies: LiveData<Boolean> = _newReplies

    private val _newZaps = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyZaps.value }
    val newZaps: LiveData<Boolean> = _newZaps

    private val _newQuotes = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyQuotes.value }
    val newQuotes: LiveData<Boolean> = _newQuotes

    private val _newReactions = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyReactions.value }
    val newReactions: LiveData<Boolean> = _newReactions

    private val _newPrivate = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyPrivate.value }
    val newPrivate: LiveData<Boolean> = _newPrivate

    private val _newMentions = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyMentions.value }
    val newMentions: LiveData<Boolean> = _newMentions

    private val _newReposts = MutableLiveData<Boolean>().apply { value = EncryptedStorage.notifyResposts.value }
    val newReposts: LiveData<Boolean> = _newReposts

    init {
        EncryptedStorage.notifyReplies.observeForever { value ->
            _newReplies.value = value
        }
        Log.d("Pokey", "_newZaps.value" + _newZaps.value)
        EncryptedStorage.notifyZaps.observeForever { value ->
            _newZaps.value = value
            Log.d("Pokey", "observeForever" + value)
        }
        EncryptedStorage.notifyQuotes.observeForever { value ->
            _newQuotes.value = value
        }
        EncryptedStorage.notifyReactions.observeForever { value ->
            _newReactions.value = value
        }
        EncryptedStorage.notifyPrivate.observeForever { value ->
            _newPrivate.value = value
        }
        EncryptedStorage.notifyMentions.observeForever { value ->
            _newMentions.value = value
        }
        EncryptedStorage.notifyResposts.observeForever { value ->
            _newReposts.value = value
        }
    }

    fun updateNotifyReplies(value: Boolean) {
        _newReplies.value = value
        EncryptedStorage.updateNotifyReplies(value)
    }

    fun updateNotifyReactions(value: Boolean) {
        _newReactions.value = value
        EncryptedStorage.updateNotifyReactions(value)
    }

    fun updateNotifyPrivate(value: Boolean) {
        _newPrivate.value = value
        EncryptedStorage.updateNotifyPrivate(value)
    }

    fun updateNotifyZaps(value: Boolean) {
        _newZaps.value = value
        Log.d("Pokey", "updateNotifyZaps" + value)
        EncryptedStorage.updateNotifyZaps(value)
    }

    fun updateNotifyQuotes(value: Boolean) {
        _newQuotes.value = value
        EncryptedStorage.updateNotifyQuotes(value)
    }

    fun updateNotifyMentions(value: Boolean) {
        _newMentions.value = value
        EncryptedStorage.updateNotifyMentions(value)
    }

    fun updateNotifyReposts(value: Boolean) {
        _newReposts.value = value
        EncryptedStorage.updateNotifyReposts(value)
    }
}
