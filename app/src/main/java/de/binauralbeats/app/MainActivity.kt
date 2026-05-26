package de.binauralbeats.app

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import de.binauralbeats.app.service.AudioPlaybackService
import de.binauralbeats.app.ui.BinauralViewModel
import de.binauralbeats.app.ui.screens.MainScreen
import de.binauralbeats.app.ui.theme.BinauralBeatsTheme

class MainActivity : ComponentActivity() {

    private val viewModel: BinauralViewModel by viewModels()
    private var audioService: AudioPlaybackService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val service = (binder as AudioPlaybackService.LocalBinder).service
            audioService = service
            viewModel.bindService(service)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
        }
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, we proceed */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestNotificationPermission()
        bindAudioService()

        handleDeepLink(intent)

        setContent {
            BinauralBeatsTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        intent?.data?.let { uri -> viewModel.importFromUri(uri) }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun bindAudioService() {
        val intent = Intent(this, AudioPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        try { unbindService(serviceConnection) } catch (_: Exception) {}
        super.onDestroy()
    }
}
