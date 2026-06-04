import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version("1.9.24-1.0.20") apply(false)
}

val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) FileInputStream(keystorePropsFile).use { load(it) }
}

android {
    namespace = "com.beatdrop.kt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.beatdrop.kt"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        vectorDrawables { useSupportLibrary = true }

    }

    signingConfigs {
        create("release") {
            val ksPath = System.getenv("KEYSTORE_FILE") ?: keystoreProps.getProperty("storeFile")
            if (ksPath != null && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProps.getProperty("storePassword")
                keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProps.getProperty("keyAlias")
                keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            val rel = signingConfigs.getByName("release")
            signingConfig = if (rel.storeFile != null) rel else signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    testOptions { unitTests.isReturnDefaultValues = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        // NewPipeExtractor uses java.nio.* APIs (Path / Files) that aren't
        // available pre-Android O. Core library desugaring backports them so
        // we can keep minSdk=24. desugar_jdk_libs_nio adds the nio backports
        // on top of the standard desugar set.
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose UI + Material 3
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Lucide stroke icons (uniform 2px stroke per Liquid Glass spec §8)
    implementation("com.composables:icons-lucide-android:1.0.0")
    // Real backdrop blur (CSS backdrop-filter equivalent for Compose).
    // 0.7.3 is the last version compatible with Compose UI 1.6.x / Kotlin 1.9.24.
    implementation("dev.chrisbanes.haze:haze-android:0.7.3")
    implementation("androidx.compose.animation:animation")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 / ExoPlayer
    val media3 = "1.3.1"
    implementation("androidx.media3:media3-exoplayer:$media3")
    implementation("androidx.media3:media3-session:$media3")
    implementation("androidx.media3:media3-common:$media3")
    implementation("androidx.media3:media3-ui:$media3")
    // Adaptive streaming support — some resolved YouTube/Invidious URLs are HLS/DASH manifests
    implementation("androidx.media3:media3-exoplayer-hls:$media3")
    implementation("androidx.media3:media3-exoplayer-dash:$media3")

    // Coil image loading
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.palette:palette-ktx:1.0.0")

    // DataStore (settings)
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Permissions in Compose
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Google Play Services Basement / Security Provider Installer
    implementation("com.google.android.gms:play-services-base:18.5.0")

    // OkHttp — for Innertube search, stream URL resolution, and downloads
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Mozilla Rhino — evaluates YouTube's base.js signature + n-throttle JS
    // (the same approach NewPipe/yt-dlp use to decipher protected stream URLs).
    // NewPipeExtractor also depends on Rhino transitively; the explicit pin
    // keeps us on a known-good version regardless of NewPipe's choice.
    implementation("org.mozilla:rhino:1.7.14")

    // ── NewPipeExtractor ────────────────────────────────────────────────────
    // The actively-maintained YouTube extractor library used by NewPipe and
    // Tubular. Self-updates against BotGuard / PO-token churn via library
    // version bumps. Pulled in from JitPack (no Maven Central release).
    // v0.26.0 (Feb 2026) is current and handles the 2025 WebView-extractor
    // breakage that took out our in-app Strategy 3.
    implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.0")
    // Required by NewPipe's nio-using paths at runtime (we already enable
    // core library desugaring above; this adds the nio backport jars on
    // top of the standard desugar set).
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")

    // Room database — download history, subscriptions
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")

    // AndroidX Activity results API (for share handling)
    implementation("androidx.activity:activity-ktx:1.9.0")

    // Gson for JSON serialization (download history, metadata)
    implementation("com.google.code.gson:gson:2.11.0")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
