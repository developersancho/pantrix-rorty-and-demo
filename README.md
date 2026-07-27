# Pantrix Rorty — Android demo

A from-scratch **MVVM / ViewBinding** sample app that integrates the published
[Pantrix Android SDK](https://github.com/developersancho/pantrix-sdk-android-aar) (`1.0.0-beta.5`) and
exercises every SDK surface, using the
[Rick & Morty REST API](https://rickandmortyapi.com/documentation#rest) as its data source. It is the
Android counterpart of the `pantrix-rorty-ios-uikit-demo` app.

Its second job is to be a **real consumer of a real release**. The Android SDK's own release checklist
names this as a known gap: step 7 only lists the files on the `maven-repo` branch, which is not the same
as something resolving and running them. This app closes that gap.

## Stack

| Concern | Choice |
| --- | --- |
| UI | Views + ViewBinding, Navigation Component, 5 bottom-nav tabs |
| minSdk / compileSdk | 24 / 37 |
| Architecture | MVVM (`ViewModel` + `StateFlow`), one repository |
| DI | Hilt 2.60.1 |
| Networking | Retrofit + OkHttp + Moshi (codegen) |
| Images | Coil |
| Telemetry | Pantrix — SDK core, OkHttp tracking, Inspector, Feedback, Widget, Gradle plugin |

---

# Wiring the SDK — step by step

Steps 1–3 are the dependency setup, 4–7 are runtime, 8–11 are the parts that only matter once
something crashes or you look at the data.

## 1. Add the Maven repository

The SDK is **not on Maven Central**. It is served as raw files from the `maven-repo` branch of the
distribution repo, so the URL has to be declared by hand — in
[`settings.gradle.kts`](settings.gradle.kts), in **both** blocks:

```kotlin
pluginManagement {
    repositories {
        /* … */
        maven {
            url = uri("https://raw.githubusercontent.com/developersancho/pantrix-sdk-android-aar/maven-repo/")
            content { includeGroupByRegex("com\\.pantrix.*") }
        }
    }
}
dependencyResolutionManagement {
    repositories {
        /* … */
        maven { url = uri("…/maven-repo/"); content { includeGroupByRegex("com\\.pantrix.*") } }
    }
}
```

Both, because the Gradle plugin (step 10) resolves through `pluginManagement` and the libraries through
`dependencyResolutionManagement` — declaring it in one place leaves the other half unresolvable.

The `content { includeGroupByRegex(…) }` filter is not decoration. Without it Gradle asks
raw.githubusercontent.com for *every* dependency in the build before falling through to Central, which
is both slow and a needless amount of traffic to a host that knows nothing about them.

## 2. Version catalog

[`gradle/libs.versions.toml`](gradle/libs.versions.toml) — one version for all Pantrix artifacts,
including the Gradle plugin, so they can never drift apart:

```toml
[versions]
pantrix = "1.0.0-beta.5"

[libraries]
pantrix-sdk            = { group = "com.pantrix.analytics", name = "pantrix-sdk",            version.ref = "pantrix" }
pantrix-inspector      = { group = "com.pantrix.analytics", name = "pantrix-inspector",      version.ref = "pantrix" }
pantrix-inspector-noop = { group = "com.pantrix.analytics", name = "pantrix-inspector-noop", version.ref = "pantrix" }
# … feedback / feedback-noop / widget / widget-noop the same way

[plugins]
pantrix-gradle = { id = "com.pantrix.gradle", version.ref = "pantrix" }
```

`pantrix-sdk` is the whole core — event pipeline, storage, crash/ANR capture **and** the OkHttp
integration used in step 7. The three debug tools ship as **twin pairs**: a real module and an
API-compatible inert `-noop`.

## 3. Declare the dependencies

[`app/build.gradle.kts`](app/build.gradle.kts):

```kotlin
implementation(libs.pantrix.sdk)                       // every variant

debugImplementation(libs.pantrix.inspector)            // real tools on the dev variants
debugImplementation(libs.pantrix.feedback)
"qaTestImplementation"(libs.pantrix.inspector)
"qaTestImplementation"(libs.pantrix.feedback)
releaseImplementation(libs.pantrix.inspector.noop)     // inert stubs on release
releaseImplementation(libs.pantrix.feedback.noop)

debugImplementation(libs.pantrix.widget)               // widget: DEBUG ONLY — see below
debugImplementation(libs.glance.appwidget)
"qaTestImplementation"(libs.pantrix.widget.noop)
releaseImplementation(libs.pantrix.widget.noop)
```

The twin pairs are what lets the same `PantrixInspector.init(…)` / `PantrixFeedback.init(…)` call sites
stay in the code unguarded: on release those calls land on the stubs, and the tools' code is not in the
shipped APK at all. No `if (BuildConfig.DEBUG)` anywhere.

**The widget needed a rule of its own, and finding out cost a latent crash.** `pantrix-widget` has no
`init` call — its `PantrixWidgetReceiver` arrives through the merged manifest, so merely having the
module on a build type makes the widget appear in the launcher's picker. And `pantrix-widget` declares
Glance as `compileOnly`, i.e. the consumer supplies it. Put the real widget on a variant without adding
Glance and nothing fails at build time — the app crashes with `NoClassDefFoundError` the moment someone
places the widget. Hence: real widget **and** Glance on debug, `-noop` everywhere else.

> `qaTest` is a custom build type with no counterpart in the SDK modules, which publish `debug`/`release`
> only. It carries `matchingFallbacks += "release"`, which the published-AAR path does not strictly need —
> AAR matching is lenient — but a local `projects.*` wiring does, and it costs nothing to keep.

## 4. One Pantrix project per build variant

Each variant installs under its own `applicationId` and reports to its **own Pantrix project**:

| variant | applicationId | Pantrix project | ingest token |
| --- | --- | --- | --- |
| `debug` | `…pantrixrortyanddemo.debug` | Rorty Dev Android | `px_1oa…n87i` |
| `qaTest` | `…pantrixrortyanddemo.test` | Rorty Test Android | `px_lxs…pvo9` |
| `release` | `…pantrixrortyanddemo` | Rorty Android | `px_f0t…dvab` |

This has to line up exactly. The ingest gate compares the incoming `build.appId` against the project's
recorded `app_id` with **exact equality**, so a token from the wrong project does not half-work — the
whole batch is rejected with `APP_ID_MISMATCH` and *nothing* arrives.

The token goes in via `buildConfigField`, per build type:

```kotlin
defaultConfig {
    // 10.0.2.2 is the emulator's alias for the host loopback; a real device needs the Mac's LAN IP.
    buildConfigField("String", "PANTRIX_URL", "\"http://10.0.2.2:8099\"")
}
buildTypes {
    release { buildConfigField("String", "PANTRIX_TOKEN", "\"px_f0t…\"") }
    debug   { buildConfigField("String", "PANTRIX_TOKEN", "\"px_1oa…\"") }
    create("qaTest") { buildConfigField("String", "PANTRIX_TOKEN", "\"px_lxs…\"") }
}
```

**An SDK ingest key in `BuildConfig` is correct, not a leak.** It ships inside the app and is extractable
from any APK by design — the backend treats it as public. The **CI** keys are the secret ones, and they
live somewhere else entirely (step 10).

`PANTRIX_URL` is the backend **origin**, not an API path: the SDK appends `/api/v1/sdk/events` and
`/api/v1/sdk/config` itself. The Gradle plugin's `apiUrl` in step 10 is the opposite convention and does
include `/api`. They are genuinely different values for the same backend.

## 5. Read the variant at runtime

[`app/BuildVariant.kt`](app/src/main/java/com/developersancho/pantrixrortyanddemo/app/BuildVariant.kt) —
an enum resolved from `BuildConfig.BUILD_TYPE`, mirroring the iOS demo's `BuildVariant.swift`:

```kotlin
enum class BuildVariant {
    DEBUG, QA_TEST, RELEASE;

    val isRelease: Boolean get() = this == RELEASE
    val backendUrl: String get() = BuildConfig.PANTRIX_URL
    val ingestToken: String get() = BuildConfig.PANTRIX_TOKEN
    val enableSdkLogging: Boolean get() = !isRelease
    val allowInsecureConnection: Boolean get() = backendUrl.startsWith("http://")

    companion object {
        val current: BuildVariant get() = when (BuildConfig.BUILD_TYPE) {
            "debug" -> DEBUG; "qaTest" -> QA_TEST; else -> RELEASE
        }
    }
}
```

One place decides what "this build" means, so the `Application` reads settings instead of branching on
build type five times.

## 6. Initialise in `Application.onCreate`

[`app/RortyApp.kt`](app/src/main/java/com/developersancho/pantrixrortyanddemo/app/RortyApp.kt):

```kotlin
@HiltAndroidApp
class RortyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val variant = BuildVariant.current

        Pantrix.init(
            context = this,
            config = PantrixConfig(token = variant.ingestToken, url = variant.backendUrl) {
                enableLogging(variant.enableSdkLogging)
                allowInsecureConnection(variant.allowInsecureConnection)
                trackHttpHeaders(true)
                trackHttpBody(true)
                retentionDays(if (variant.isRelease) 30 else 0)      // 0 = unlimited
                maxStoredEvents(if (variant.isRelease) 50_000 else 0)
                keepSentEvents(!variant.isRelease)
                storageEncryption(if (variant.isRelease) StorageEncryption.FULL else StorageEncryption.NONE)
                enableRemoteConfig(true)
            }
        )
        /* … step 7 … */
    }
}
```

As early as possible, because `autoStart` defaults to true: collection begins immediately and
Activities/Fragments are screen-tracked by class name from the first frame — no per-screen call needed.

The knobs that are not obvious:

- **`allowInsecureConnection`** — required for a plain `http://` backend. The SDK refuses cleartext
  otherwise, and a real production backend would be HTTPS and would not need this.
- **`retentionDays` / `maxStoredEvents`** — `0` means **unlimited** on both, not "keep nothing". The dev
  variants keep everything so the on-device Inspector has something to show.
- **`keepSentEvents(!isRelease)`** — off means uploaded events are deleted from the local store. Since the
  Inspector reads that same store, turning it off is exactly what makes events look like they are
  "disappearing" from the Inspector.
- **`enableRemoteConfig(true)`** — the dashboard's SDK Config screen may override the values above at the
  next launch. Set it `false` to make this local config authoritative.

## 7. Debug tools

Same `onCreate`, right after `Pantrix.init`:

```kotlin
PantrixInspector.init(this, InspectorConfig(
    showFloatingButton = !variant.isRelease,
    enableShakeGesture = false,   // Feedback owns the shake — two listeners on one shake race
))
PantrixFeedback.init(this, FeedbackConfig(recipientEmail = "…"))
```

Unguarded on purpose — on release these are the `-noop` twins from step 3.

The widget needs no init at all (step 3).

## 8. HTTP tracking through OkHttp

[`di/NetworkModule.kt`](app/src/main/java/com/developersancho/pantrixrortyanddemo/di/NetworkModule.kt) —
the **only** place Pantrix touches the app's networking:

```kotlin
@Provides @Singleton
fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
    .apply {
        Pantrix.getOkHttpEventCollector()?.let { collector ->
            addInterceptor(PantrixOkHttpApplicationInterceptor(collector))
            eventListenerFactory(PantrixEventListenerFactory(collector = collector, delegate = null))
        }
    }
    .build()
```

**Both halves are needed and they do different jobs.** The interceptor sees the request/response pair —
method, url, status, bodies. The event listener supplies the timings the interceptor cannot observe (DNS,
connect, TLS, first byte). One without the other gives you half an HTTP event.

`getOkHttpEventCollector()` returns `null` when HTTP tracking is unavailable, so the `?.let` degrades to a
plain client instead of failing. Everything downstream — Retrofit, the repository, all five tabs — is
ordinary code that has never heard of Pantrix.

## 9. Manifest

```xml
<uses-permission android:name="android.permission.INTERNET" />

<application
    android:name=".app.RortyApp"
    android:usesCleartextTraffic="true">
```

`usesCleartextTraffic` is the platform-side twin of `allowInsecureConnection`: **both** are required to
talk to a plain-http local backend, and they fail in different places if you only do one.

## 10. R8 mapping upload (release + qaTest)

A minified Android stack trace is meaningless without the R8 `mapping.txt`. The
[`com.pantrix.gradle`](https://github.com/developersancho/pantrix-sdk-android-aar) plugin embeds a
content-UUID of the mapping into the APK's assets and uploads the mapping under that same id, so the
backend can match a crash to its mapping without guessing.

```kotlin
plugins { alias(libs.plugins.pantrix.gradle) }

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
        apiUrl = "http://localhost:8099/api"
        enabled = name != "debug" && key.isNotEmpty()
    }
}
```

Four things here are each a trap:

- **CI keys are secrets and never go in `BuildConfig`.** They live in the gitignored `local.properties`
  (or `PANTRIX_CI_KEY_RELEASE` / `PANTRIX_CI_KEY_QATEST` in the environment on a build machine) and never
  reach the APK. This is the opposite of the ingest key in step 4 — the two key types are deliberately
  different (`key_type=CI` vs `SDK`).
- **`localhost`, not `10.0.2.2`.** This task runs on the **build machine**. `10.0.2.2` is the emulator's
  alias for the host loopback and means nothing outside it. Getting these the same way round costs a
  build-long socket timeout with no useful error.
- **The mapping project must be the same project the crashes go to.** Each variant uploads with its own
  CI key; a mapping in the wrong project leaves the crash obfuscated, and nothing says so.
- **`enabled = name != "debug"`.** Debug is not minified, so R8 never runs and there is no `mapping.txt`
  to upload. The `key.isNotEmpty()` half means a machine without credentials builds fine instead of
  failing.

Tasks: `pantrixGenerateMappingUuid<Variant>` → `pantrixInjectDebugMeta<Variant>` →
`pantrixUploadMapping<Variant>`, the last finalising `assemble<Variant>`/`bundle<Variant>`.

Add to `local.properties`:

```properties
pantrix.ci.key.release=px_…
pantrix.ci.key.qaTest=px_…
# No debug entry on purpose — debug is not minified.
```

## 11. Keep screen names readable under R8

[`app/proguard-rules.pro`](app/proguard-rules.pro):

```proguard
-keepnames class * extends androidx.fragment.app.Fragment
-keepnames class * extends android.app.Activity
```

Automatic screen tracking reports the **class name**. Without this, a minified build reports screens
called `a`, `b`, `c` — technically correct and completely unusable in a dashboard. (Crash frames are
handled by step 10; this is about screen names, which are not deobfuscated on the backend.)

---

# Build & run

```bash
./gradlew :app:assembleQaTest
```

```bash
./gradlew :app:installQaTest
```

Needs a Pantrix TEST backend on `http://localhost:8099`, reachable from the emulator at `10.0.2.2:8099`.
A physical device needs the Mac's LAN IP in `PANTRIX_URL` instead (and the TEST backend publishes 8099 on
`127.0.0.1` only, so it also needs a forwarder bound to that IP).

| variant | app label | minified | mapping upload |
| --- | --- | --- | --- |
| `debug` | RT Deb | no | no (nothing to upload) |
| `qaTest` | RT Test | yes | yes |
| `release` | RT | yes | yes |

# Verifying the integration

1. **On the device** — the Inspector's floating button (dev variants) lists events, HTTP calls, screens
   and crashes straight from the local store.
2. **In the dashboard** — the variant's project should show sessions, screen views and HTTP events.
   Note that a change made in the dashboard's SDK Config screen reaches the device only on the **next app
   launch**.
3. **Crash path** — Crash Lab triggers a crash, the app is relaunched (fatal crashes are reported on the
   next launch), and the crash should appear in the rollup **deobfuscated**.

   The failure mode to know about: on a minified variant the plugin stamps a mapping id into the APK, so
   if the mapping itself never arrives the backend marks the crash `symbolication.status = "missing"` and
   the crash rollup **filters it out entirely**. It is in the events table and absent from the crash list
   — which reads like the crash was never captured. (A debug crash carries no mapping id at all, so it
   passes straight through and shows up unobfuscated.)

# App structure

```
app/
  MainActivity.kt            single Activity, Navigation Component, 5-tab bottom nav
  app/                       RortyApp (init), BuildVariant
  di/NetworkModule.kt        Hilt — Moshi, OkHttp (+ Pantrix), Retrofit
  network/                   RickMortyApi + Moshi models
  feature/
    shared/                  PagedListFragment/ViewModel, RowAdapter, RickMortyRepository
    characters/              list + detail (detail lists the episodes the character appears in)
    episodes/                list + detail (detail lists the characters in the episode)
    locations/               list + detail (detail lists the residents)
    lab/                     LabFragment (one row per SDK surface) + CrashLabFragment
    profile/                 user identity actions + a "this build" summary
```

# Known gaps

- **`connectionType=not_connected` on the emulator.** Export works, so the transport is fine — the
  connectivity detector does not see the emulator's network and the metadata on every event is wrong.
- **Periodic performance events are attributed to `NavHostFragment`**, not the visible fragment, because
  Navigation Component hosts screens in a child fragment manager.
- **No `clearUser()` on Android.** iOS has it; the Android public API has no equivalent, so the Profile
  screen can unset individual properties but cannot de-identify a user. There is no "Log out" row here for
  that reason.

The first two are SDK-side findings raised by this demo, not app bugs.
