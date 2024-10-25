package com.koalasat.pokey.models

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.koalasat.pokey.Pokey
import com.vitorpamplona.quartz.signers.ExternalSignerLauncher
import com.vitorpamplona.quartz.signers.SignerType
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class ExternalSigner(fragment: Fragment) {
    private var nostrSignerLauncher: ActivityResultLauncher<Intent>
    private var externalSignerLauncher: ExternalSignerLauncher = ExternalSignerLauncher("", signerPackageName = "")

    init {
        nostrSignerLauncher = fragment.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                Log.e("Pokey", "ExternalSigner result error: ${result.resultCode}")
            } else {
                result.data?.let { externalSignerLauncher.newResult(it) }
            }
        }
        externalSignerLauncher.registerLauncher(
            launcher = {
                try {
                    nostrSignerLauncher.launch(it) // This can remain if you still need to launch it
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    Log.e("Pokey", "Error opening Signer app", e)
                }
            },
            contentResolver = { Pokey.getInstance().contentResolverFn() },
        )
    }

    fun savePubKey() {
        externalSignerLauncher.openSignerApp(
            "",
            SignerType.GET_PUBLIC_KEY,
            "",
            UUID.randomUUID().toString(),
        ) { result ->
            val split = result.split("-")
            val pubkey = split.first()
            if (split.first().isNotEmpty()) EncryptedStorage.updatePubKey(pubkey)
        }
    }
}
