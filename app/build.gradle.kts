plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.bagis"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.bagis"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.5"
    }

    // Sabit imza: her derlemede aynı anahtar kullanılır, böylece yeni sürüm
    // eskisinin üstüne kurulur ve telefondaki veriler silinmez.
    signingConfigs {
        getByName("debug") {
            storeFile = file("../bagis.keystore")
            storePassword = "bagis1234"
            keyAlias = "bagis"
            keyPassword = "bagis1234"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
}
