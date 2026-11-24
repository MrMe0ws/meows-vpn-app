plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "app.meows.vpn"
    compileSdk = 36

    defaultConfig {
        applicationId = "app.meows.vpn"
        minSdk = 24
        targetSdk = 35
        versionCode = 19
        versionName = "1.11.1"

        // Универсальная APK: ядро Xray лежит в jniLibs/<abi>/libxkcore.so для каждой архитектуры.
        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86") }
    }

    // direct — APK для GitHub/сайта (с автообновлением и ссылками магазина); play — AAB для Google Play (без них, см. docs/play.md)
    flavorDimensions += "store"
    productFlavors {
        create("direct") {
            dimension = "store"
            buildConfigField("boolean", "PLAY", "false")
        }
        create("play") {
            dimension = "store"
            targetSdk = 36   // Google Play требует API 36 для новых приложений с 31.08.2026
            buildConfigField("boolean", "PLAY", "true")
        }
    }

    signingConfigs {
        create("meows") {
            val ks = System.getenv("MEOWS_KEYSTORE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("MEOWS_KEYPASS")
                keyAlias = "meows"
                keyPassword = System.getenv("MEOWS_KEYPASS")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (System.getenv("MEOWS_KEYSTORE") != null) signingConfig = signingConfigs.getByName("meows")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }

    // Ядро запускается как процесс прямо из nativeLibraryDir, поэтому .so должны быть распакованы на диск
    packaging {
        jniLibs {
            useLegacyPackaging = true
            keepDebugSymbols += "**/libxkcore.so"
        }
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    androidResources {
        localeFilters += listOf("ru", "en")
        noCompress += "dat"   // geoip/geosite читаются через openFd — в APK не должны быть сжаты
    }
    lint { abortOnError = false }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    // Expressive API (LoadingIndicator, WavyProgress, MotionScheme.expressive) — нужна 1.5.0-alpha (в 1.4.0 stable они скрыты)
    implementation("androidx.compose.material3:material3:1.5.0-alpha04")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.animation:animation")

    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Палитры Material You из seed-цвета (для тем, кроме системной «из обоев»)
    implementation("com.materialkolor:material-kolor:3.0.1")

    // Сканер QR-кодов подписки (без Google Play Services)
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
}
