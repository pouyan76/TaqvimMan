
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")

val releaseKeystoreFile = layout.buildDirectory.file("release/taqvimyar-plus.jks")

if (!keystoreBase64.isNullOrBlank()) {
    val keystoreBytes = Base64.getDecoder().decode(keystoreBase64)
    val keystore = releaseKeystoreFile.get().asFile
    keystore.parentFile.mkdirs()
    keystore.writeBytes(keystoreBytes)
}

android {
    namespace = "ir.taqvimm.an"
    compileSdk = 35

    defaultConfig {
        applicationId = "ir.taqvimm.an"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        create("release") {
            if (!keystoreBase64.isNullOrBlank()) {
                storeFile = releaseKeystoreFile.get().asFile
                storePassword = keystorePassword
                keyAlias = "taqvimyar"
                keyPassword = keystorePassword
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (!keystoreBase64.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-session:1.5.1")
}
