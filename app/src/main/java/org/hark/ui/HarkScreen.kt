package org.hark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.hark.appContainer
import org.hark.audio.displayName
import androidx.compose.ui.platform.LocalLocale

private val Colors = darkColorScheme(
    primary = Color(0xFF7FD8F0),
    background = Color(0xFF0B1016),
    surface = Color(0xFF121A23),
    surfaceVariant = Color(0xFF16212C),
)

@Composable
fun HarkScreen(accessGranted: StateFlow<Boolean>, onOpenAccess: () -> Unit, onOpenAppInfo: () -> Unit) {
    val container = LocalContext.current.appContainer
    val settings by container.settings.settings.collectAsStateWithLifecycle()
    val bluetooth by container.bluetooth.current.collectAsStateWithLifecycle()
    val lastEvent by container.speaker.lastEvent.collectAsStateWithLifecycle()
    val granted by accessGranted.collectAsStateWithLifecycle()
    val scope = container.scope

    MaterialTheme(colorScheme = Colors) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.safeDrawingPadding().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Hark", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Reads new text messages aloud, only through Bluetooth audio. With no Bluetooth device connected, nothing is spoken.",
                    style = MaterialTheme.typography.bodyMedium,
                )

                Section {
                    ToggleRow("Read messages aloud", settings.enabled) { scope.launch { container.settings.setEnabled(it) } }
                }

                Section {
                    Text("Status", style = MaterialTheme.typography.titleMedium)
                    if (granted) {
                        Text("✓ Notification access granted")
                    } else {
                        Text("Hark needs notification access to see incoming messages.")
                        Button(onClick = onOpenAccess) { Text("Grant notification access") }
                        Text(
                            "If Android says the setting is restricted (common for apps installed outside the Play Store), " +
                                "open App info, tap ⋮ and choose \"Allow restricted settings\", then try again.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        OutlinedButton(onClick = onOpenAppInfo) { Text("Open App info") }
                    }
                    Text(
                        bluetooth?.let { "✓ Bluetooth audio: ${it.displayName}" } ?: "✗ No Bluetooth audio connected: messages stay silent",
                    )
                    lastEvent?.let { Text("Last: $it", style = MaterialTheme.typography.bodySmall) }
                }

                Section {
                    Text("Read messages from", style = MaterialTheme.typography.titleMedium)
                    ToggleRow("Text messages (SMS / Google Messages)", settings.readSms) { scope.launch { container.settings.setReadSms(it) } }
                    ToggleRow("Google Voice (texts and voicemail)", settings.readVoice) { scope.launch { container.settings.setReadVoice(it) } }
                }

                Section {
                    var rate by remember(settings.speechRate) { mutableFloatStateOf(settings.speechRate) }
                    Text("Speech rate: ${String.format(LocalLocale.current.platformLocale, "%.1f", rate)}×", style = MaterialTheme.typography.titleMedium)
                    Slider(
                        value = rate,
                        onValueChange = { rate = it },
                        onValueChangeFinished = { scope.launch { container.settings.setSpeechRate(rate) } },
                        valueRange = 0.5f..2.0f,
                        steps = 14,
                    )
                    Button(onClick = {
                        container.speaker.speak("This is Hark. New messages will be read here.", "Test message")
                    }) { Text("Speak a test message") }
                }
            }
        }
    }
}

@Composable
private fun Section(content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
