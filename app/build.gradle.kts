import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "org.hark"
    compileSdk {
        version = release(37) { minorApiLevel = 2 }
    }
    defaultConfig {
        applicationId = "org.hark"
        // Route verification (MediaPlayer.setPreferredDevice + routing callbacks) needs API 28.
        minSdk = 28
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.1"
    }
    buildFeatures { compose = true }

    // Release signing: the key lives outside the repository, in
    // ~/.android-keys/hark-release.properties (or the file named by the Gradle property
    // hark.signing). Without it, release builds are left unsigned.
    val signingFile = (findProperty("hark.signing") as String?)?.let(::file)
        ?: file("${System.getProperty("user.home")}/.android-keys/hark-release.properties")
    val signing = Properties().apply {
        if (signingFile.exists()) signingFile.inputStream().use(::load)
    }
    signingConfigs {
        if (signing.isNotEmpty()) {
            create("release") {
                storeFile = file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("storePassword")
                keyAlias = signing.getProperty("keyAlias")
                keyPassword = signing.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        debug { applicationIdSuffix = ".debug" }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }
    lint { abortOnError = true }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
}
