plugins {
    id("com.android.application")
}

android {
    namespace = "com.dnmarine.muhasebe"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.dnmarine.muhasebe"
        minSdk = 26
        targetSdk = 35
        versionCode = 74
        versionName = "2.22.0"
    }

    // Sabit debug anahtarı: her CI derlemesi aynı imzayı taşır, yeni APK eskisinin üzerine kurulur (veriler korunur).
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    // Debug imzalı olsa da APK hata ayıklanabilir değildir: USB ile bağlanan biri uygulama verisini (oturum jetonu, kayıtlar) okuyamaz.
    buildTypes {
        getByName("debug") {
            isDebuggable = false
        }
    }

    // Uygulamanın tamamı tek dosya: web/index.html. APK onu WebView içinde açar.
    sourceSets {
        getByName("main") {
            assets.srcDir("../web")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
