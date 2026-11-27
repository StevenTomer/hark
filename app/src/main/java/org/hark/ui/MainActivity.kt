package org.hark.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val accessGranted = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HarkScreen(
                accessGranted = accessGranted,
                onOpenAccess = ::openNotificationAccess,
                onOpenAppInfo = ::openAppInfo,
            )
        }
    }

    override fun onResume() {
        super.onResume()
        accessGranted.value = packageName in NotificationManagerCompat.getEnabledListenerPackages(this)
    }

    private fun openNotificationAccess() {
        runCatching { startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
            .onFailure { openAppInfo() }
    }

    private fun openAppInfo() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}
