plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
    kotlin("plugin.serialization")
    alias(libs.plugins.google.service)
}

android {
    namespace = "best.app.offlinehisab"
    compileSdk = 36

    defaultConfig {
        applicationId = "best.app.offlinehisab"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    // 🚀 FIX: Exclude duplicate META-INF files
    packaging {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md",
                "META-INF/INDEX.LIST",
                "META-INF/ASL2.0"
            )
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Room
    implementation(libs.androidx.room.runtime)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)


    // Coroutines
    implementation(libs.kotlinx.coroutines.android)


    // Optional - for previewing
    debugImplementation(libs.ui.tooling)

    // Firebase
    implementation(platform(libs.firebase.bom)) // choose a current BOM version
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.storage.ktx)

    // Coroutines helpers for Tasks (optional but convenient)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.coroutines.android)


    implementation(libs.play.services.auth)
    implementation(libs.google.api.services.drive)
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)


}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:31.1-android")
    }
}