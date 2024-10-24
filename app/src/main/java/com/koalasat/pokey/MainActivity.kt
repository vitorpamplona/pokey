package com.koalasat.pokey

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.koalasat.pokey.databinding.ActivityMainBinding
import com.koalasat.pokey.models.EncryptedStorage
import com.vitorpamplona.quartz.signers.ExternalSignerLauncher
import com.vitorpamplona.quartz.signers.SignerType
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class MainActivity : AppCompatActivity() {
    private val requestCodePostNotifications: Int = 1
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EncryptedStorage.init(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navView: BottomNavigationView = binding.navView

        val navController = findNavController(R.id.nav_host_fragment_activity_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home,
                R.id.navigation_relays,
                R.id.navigation_notifications,
            ),
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                requestCodePostNotifications,
            )
        }

        if (EncryptedStorage.pubKey.value.isNullOrEmpty()) connectExternalSigner()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodePostNotifications) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
            } else {
                Toast.makeText(applicationContext, getString(R.string.permissions_required), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectExternalSigner() {
        val id = UUID.randomUUID().toString()
        val externalSignerLauncher = ExternalSignerLauncher("", signerPackageName = "")

        val nostrSignerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        externalSignerLauncher.openSignerApp(
            "",
            SignerType.GET_PUBLIC_KEY,
            "",
            id,
        ) { result ->
            val split = result.split("-")
            val pubkey = split.first()
            if (split.first().isNotEmpty()) EncryptedStorage.updatePubKey(pubkey)
        }
    }
}
