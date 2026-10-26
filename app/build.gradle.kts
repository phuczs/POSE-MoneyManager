import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    kotlin("kapt")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.moneymanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "pose.moneymanager"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        val apiKey = properties.getProperty("apiKey") ?: ""

        // Tạo biến BuildConfig.apiKey để dùng trong code Kotlin
        buildConfigField("String", "apiKey", "\"$apiKey\"")

        // Gemini API Key — stored in local.properties (git-ignored), NEVER hardcode here.
        // To use: add 'geminiApiKey=AIza...' to local.properties
        val geminiApiKey = properties.getProperty("geminiApiKey") ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
    }
}

dependencies {
    // Core Library Desugaring for Java 8+ APIs on older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) // Keep one, remove the other
    implementation(libs.firebase.storage)
    debugImplementation(libs.androidx.ui.tooling) // You also have this below, it's a duplicate

    // Navigation
    implementation(libs.navigation.compose)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)

    // Google Play Services
    implementation(libs.google.play.services.auth)
    implementation(libs.google.play.services.base)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Coroutines
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2") // Hoặc phiên bản mới hơn

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)

    // Charts for visualization
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.valentinilk.shimmer:compose-shimmer:1.3.0")

    // Icons
    implementation("androidx.compose.material:material-icons-extended")
    // Thêm SDK của Google AI (Gemini)
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")

    // Image Loading
    implementation(libs.coil.compose)

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Biometric & Process Lifecycle
    implementation("androidx.biometric:biometric-ktx:1.2.0-alpha05")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.room.paging)
    kapt(libs.room.compiler)

    // Paging 3
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
