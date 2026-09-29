plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.xpspeak.app"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.xpspeak.app"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            isCrunchPngs = false
            // Backend de Lecciones corriendo en la Mac (npm run dev:app en xp-speak-auth-backend).
            // Se usa localhost + `adb reverse tcp:3000 tcp:3000` (emulador o teléfono físico):
            // con targetSdk 37 Android bloquea a las apps las IPs de red local como 10.0.2.2.
            val leccionesBaseUrl = (project.findProperty("leccionesBaseUrl") as String?) ?: "http://localhost:3000/"
            buildConfigField("String", "LECCIONES_BASE_URL", "\"$leccionesBaseUrl\"")
        }
        release {
            buildConfigField("String", "LECCIONES_BASE_URL", "\"https://xp-speak-auth-backend.vercel.app/\"")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("dagger.fastInit", "enabled")
    arg("dagger.formatGeneratedSource", "disabled")
    arg("room.generateKotlin", "true")
}

dependencies {
    // Compose BOM (versiones alineadas)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)

    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Navigation Compose
    implementation(libs.androidx.navigation.compose)

    // Hilt (inyección de dependencias - Cap. 3.4.3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room (base de datos local - Cap. 3.4.6)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager (envía los intentos pendientes al volver la conexión)
    implementation(libs.androidx.work.runtime.ktx)

    // OkHttp (red base - módulo NetworkModule)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Retrofit & Gson
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.gson)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}