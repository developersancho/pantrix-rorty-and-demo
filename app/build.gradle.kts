import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.pantrix.gradle)
}

// R8 mapping upload. The CI keys are secrets, so they come from the gitignored local.properties
// (or the environment on a build machine) — never from BuildConfig, which ships inside the APK.
// Each variant uploads to its OWN project: the mapping project must match the crash project, or the
// backend has no mapping for the build that crashed and stack traces stay obfuscated.
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.let { load(FileInputStream(it)) }
}
fun ciKey(variant: String): String =
    (localProps["pantrix.ci.key.$variant"] as String?)
        ?: providers.environmentVariable("PANTRIX_CI_KEY_${variant.uppercase()}").orNull
        ?: ""

pantrix {
    variantFilter {
        val key = ciKey(name)
        apiKey = key
        // localhost, NOT 10.0.2.2: this task runs on the BUILD MACHINE. 10.0.2.2 is the emulator's
        // alias for the host loopback and only means anything from inside the emulator — the SDK's
        // runtime url uses that, this does not. Getting them the same way round costs a build-long
        // socket timeout.
        apiUrl = "http://localhost:8099/api"
        // debug is not minified — R8 never runs, so no mapping.txt exists to upload. An absent key
        // also disables the variant rather than failing the build on a machine that has no creds.
        enabled = name != "debug" && key.isNotEmpty()
    }
}

android {
    namespace = "com.developersancho.pantrixrortyanddemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.developersancho.pantrixrortyanddemo"
        // The emulator reaches the host's loopback at 10.0.2.2 (a real device needs the Mac's LAN IP).
        // Every variant points at the local TEST backend today; only the release arm changes when a
        // production backend exists.
        buildConfigField("String", "PANTRIX_URL", "\"http://10.0.2.2:8099\"")
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("signingConfigRelease") {
            val keystorePropertiesFile = project.rootProject.file("signing/release.signing.properties")
            if (!keystorePropertiesFile.exists()) {
                System.err.println("📜 Missing release.signing.properties file for release signing")
            } else {
                val keystoreProperties = Properties().apply {
                    load(FileInputStream(keystorePropertiesFile))
                }
                try {
                    storeFile =
                        project.rootProject.file(keystoreProperties["storeFile"] as String)
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                } catch (e: Exception) {
                    System.err.println("📜 release.signing.properties file is malformed")
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("signingConfigRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            resValue("string", "app_name", "RT")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_f0t6wrgtoorbicx3sgcocib6y0ijon8advab\"")
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "RT Deb")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_1oa68gya9p7js7y2mrmcbkuxnzzcq1okn87i\"")
        }

        create("qaTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            isDebuggable = false
            // qaTest is a custom build type with no counterpart in the SDK library
            // modules (they only have debug/release). With the `projects.*` deps,
            // variant-aware resolution needs an explicit fallback, or every project
            // dependency FAILS to resolve for this variant (IDE sync shows "Failed to
            // resolve: project :pantrix-*"). The published-AAR path matched leniently;
            // project deps don't. Fall back to the modules' release variant — the same
            // variant the published AARs resolved to, so qaTest behaviour is unchanged.
            matchingFallbacks += "release"
            resValue("string", "app_name", "RT Test")
            buildConfigField("String", "PANTRIX_TOKEN", "\"px_lxsp3ls5eej7wcota4zhtvl2k42hz6s4pvo9\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        resValues = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.moshi)
    ksp(libs.moshi.codegen)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.coil)

    // Pantrix. The debug tools are twin pairs: the real module on debug/qaTest, the inert `-noop`
    // on release, so the tool's code is not in the shipped APK at all.
    implementation(libs.pantrix.sdk)
    debugImplementation(libs.pantrix.inspector)
    debugImplementation(libs.pantrix.feedback)
    "qaTestImplementation"(libs.pantrix.inspector)
    "qaTestImplementation"(libs.pantrix.feedback)
    releaseImplementation(libs.pantrix.inspector.noop)
    releaseImplementation(libs.pantrix.feedback.noop)

    // The home-screen widget is DEBUG only. It has no init call — its receiver arrives through the
    // merged manifest — so simply having the real module on a build type makes the widget appear in
    // the launcher's picker. That is why Glance must come with it: pantrix-widget declares Glance
    // `compileOnly`, and without it the system hits NoClassDefFoundError the moment the widget is
    // placed. qaTest and release take the `-noop` twin, which registers nothing.
    debugImplementation(libs.pantrix.widget)
    debugImplementation(libs.glance.appwidget)
    "qaTestImplementation"(libs.pantrix.widget.noop)
    releaseImplementation(libs.pantrix.widget.noop)

    // SQLCipher, on exactly the variants that ask for `StorageEncryption.FULL`
    // (`BuildVariant.encryptStorage`). The SDK declares it `compileOnly`, so the app supplies it —
    // and if it doesn't, `Pantrix.init` throws, catches, logs "the SDK is disabled" and carries on.
    // Release also sets `enableLogging(false)`, so that line never appears: the app runs perfectly
    // and reports nothing at all. Measured — the release project had zero rows in ClickHouse while
    // debug and qaTest were fine. Keep this list and `encryptStorage` in step.
    //
    // ~7 MB of native libraries per ABI, which is why debug does not carry it.
    "qaTestImplementation"(libs.sqlcipher)
    releaseImplementation(libs.sqlcipher)
}