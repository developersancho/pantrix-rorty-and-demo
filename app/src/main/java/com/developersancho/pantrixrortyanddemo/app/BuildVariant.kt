package com.developersancho.pantrixrortyanddemo.app

import com.developersancho.pantrixrortyanddemo.BuildConfig

/**
 * Build-variant environment — the Android analogue of the iOS demo's `BuildVariant.swift`.
 *
 * Each variant installs under its OWN applicationId (see `app/build.gradle.kts`) and is its OWN
 * Pantrix project. The ingest gate compares the incoming `build.appId` against the project's
 * recorded `app_id` with EXACT equality, so a token from the wrong project does not half-work — the
 * whole batch is rejected with APP_ID_MISMATCH and nothing arrives:
 *
 *     variant   applicationId                                     Pantrix project
 *     debug     com.developersancho.pantrixrortyanddemo.debug     Rorty Dev Android
 *     qaTest    com.developersancho.pantrixrortyanddemo.test      Rorty Test Android
 *     release   com.developersancho.pantrixrortyanddemo           Rorty Android
 *
 * The ingest token comes from `BuildConfig` on purpose: an SDK key ships inside the app and is
 * extractable from any APK, so it is public by design. The CI keys — which upload the R8 mapping and
 * must stay secret — live in the gitignored `local.properties` instead and never reach the APK.
 */
enum class BuildVariant {
    DEBUG,
    QA_TEST,
    RELEASE;

    val isRelease: Boolean get() = this == RELEASE

    /**
     * Backend base URL. `10.0.2.2` is the emulator's alias for the host loopback; a real device
     * needs the Mac's LAN IP instead (and the TEST backend publishes 8099 on 127.0.0.1 only, so it
     * also needs a forwarder bound to that IP).
     *
     * Note this is the RUNTIME url, seen from inside the emulator. The R8 mapping upload runs on the
     * build machine and uses `localhost` — same host, different vantage point.
     */
    val backendUrl: String get() = BuildConfig.PANTRIX_URL

    /** SDK ingest token for this variant's Pantrix project. Must match the variant's applicationId. */
    val ingestToken: String get() = BuildConfig.PANTRIX_TOKEN

    /** Verbose SDK logging is for development only — in release it is noise the user pays for. */
    val enableSdkLogging: Boolean get() = !isRelease

    /** `http://` needs the insecure-connection opt-in; a real production backend would be HTTPS. */
    val allowInsecureConnection: Boolean get() = backendUrl.startsWith("http://")

    /**
     * Whether the event store is encrypted (`StorageEncryption.FULL`) rather than plaintext.
     *
     * **Deliberately not `isRelease`.** FULL needs SQLCipher, which the SDK declares `compileOnly` —
     * so the app must ship `net.zetetic:sqlcipher-android` itself. While that pairing was release-only
     * the encrypted path was the one path nobody ran, and in the Compose demo it turned out to be
     * broken: `Pantrix.init` threw `IllegalStateException: storageEncryption=FULL requires the
     * SQLCipher dependency`, caught it, logged `the SDK is disabled` — and release sets
     * `enableLogging(false)`, so even that line was invisible. Release shipped with the SDK entirely
     * off and the backend simply had zero rows for that project.
     *
     * qaTest now carries the same storage mode as release, so the variant that gets tested is the one
     * that ships. Debug stays plaintext: it iterates fastest and its db stays readable over adb, and
     * SQLCipher is ~7 MB of native libraries per ABI.
     */
    val encryptStorage: Boolean get() = this != DEBUG

    companion object {
        val current: BuildVariant
            get() = when (BuildConfig.BUILD_TYPE) {
                "debug" -> DEBUG
                "qaTest" -> QA_TEST
                else -> RELEASE
            }
    }
}
