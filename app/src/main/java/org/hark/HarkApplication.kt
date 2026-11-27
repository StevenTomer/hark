package org.hark

import android.app.Application
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import org.hark.audio.BluetoothOutput
import org.hark.audio.Speaker
import org.hark.data.SettingsRepository
import org.hark.domain.Deduper

class HarkApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this, MainScope())
    }
}

class AppContainer(context: Context, val scope: CoroutineScope) {
    val settings = SettingsRepository(context, scope)
    val bluetooth = BluetoothOutput(context)
    val speaker = Speaker(context, bluetooth) { settings.settings.value.speechRate }
    val deduper = Deduper()
}

val Context.appContainer: AppContainer get() = (applicationContext as HarkApplication).container
