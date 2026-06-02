import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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
        versionCode = 2
        versionName = "1.1.0"
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
    // (the same approach NewPipe/yt-dlp use to decipher protected stream URLs)
    implementation("org.mozilla:rhino:1.7.14")

    // Unit tests
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
