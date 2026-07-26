import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.developersancho.pantrixrortyanddemo"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.developersancho.pantrixrortyanddemo"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        }

        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            resValue("string", "app_name", "RT Deb")
        }

        create("qaTest") {
            initWith(getByName("release"))
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            isDebuggable = true
            // qaTest is a custom build type with no counterpart in the SDK library
            // modules (they only have debug/release). With the `projects.*` deps,
            // variant-aware resolution needs an explicit fallback, or every project
            // dependency FAILS to resolve for this variant (IDE sync shows "Failed to
            // resolve: project :pantrix-*"). The published-AAR path matched leniently;
            // project deps don't. Fall back to the modules' release variant — the same
            // variant the published AARs resolved to, so qaTest behaviour is unchanged.
            matchingFallbacks += "release"
            resValue("string", "app_name", "RT Test")
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}