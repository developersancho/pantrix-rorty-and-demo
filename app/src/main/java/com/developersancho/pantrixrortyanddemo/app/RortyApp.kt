package com.developersancho.pantrixrortyanddemo.app

import android.app.Application
import com.pantrix.api.Pantrix
import com.pantrix.api.PantrixConfig
import com.pantrix.core.config.StorageEncryption
import com.pantrix.feedback.api.FeedbackConfig
import com.pantrix.feedback.api.PantrixFeedback
import com.pantrix.inspector.api.InspectorConfig
import com.pantrix.inspector.api.PantrixInspector
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RortyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        val variant = BuildVariant.current

        // Initialize as early as possible: autoStart defaults to true, so collection begins
        // immediately and Activities/Fragments are screen-tracked by class name from the first frame.
        Pantrix.init(
            context = this,
            config = PantrixConfig(
                token = variant.ingestToken,
                url = variant.backendUrl
            ) {
                enableLogging(variant.enableSdkLogging)
                allowInsecureConnection(variant.allowInsecureConnection)
                trackHttpHeaders(true)
                trackHttpBody(true)
                // 0 = unlimited on both. The dev variants keep everything so the Inspector has
                // something to show; release prunes.
                retentionDays(if (variant.isRelease) 30 else 0)
                maxStoredEvents(if (variant.isRelease) 50_000 else 0)
                // Keep sent events on the dev variants so the Inspector still lists them after
                // export; dropping them is what makes events "disappear" from it.
                keepSentEvents(!variant.isRelease)
                // FULL needs SQLCipher on the classpath (the SDK declares it `compileOnly`); this app
                // ships it on the same variants — see `BuildVariant.encryptStorage` for the failure
                // that made the pairing explicit. Get it wrong and `init` disables the SDK silently.
                storageEncryption(
                    if (variant.encryptStorage) StorageEncryption.FULL else StorageEncryption.NONE
                )
                // On: the dashboard's SDK Config screen can override the values above. Set false to
                // make this local config authoritative.
                enableRemoteConfig(true)
            }
        )

        // Debug tools. On release these calls hit the `-noop` twins linked in place of the real
        // modules (see app/build.gradle.kts), so the tools' code is not in the shipped APK at all.
        PantrixInspector.init(
            context = this,
            config = InspectorConfig(
                showFloatingButton = !variant.isRelease,
                // Feedback owns the shake gesture; two listeners on one shake would race.
                enableShakeGesture = false
            )
        )
        PantrixFeedback.init(
            context = this,
            config = FeedbackConfig(recipientEmail = "developersanchez1903@gmail.com")
        )
    }
}
