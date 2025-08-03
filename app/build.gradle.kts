plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)

    id("kotlin-kapt")


}

android {
    namespace = "com.juan.reproductormusica"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.juan.reproductormusica"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }

    buildFeatures {
        compose = true
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.media)


    // ROOM
    implementation(libs.roomRuntime)
    kapt(libs.roomCompiler)

// LIFECYCLE & VIEWMODEL
    implementation(libs.lifecycleLivedata)
    implementation(libs.lifecycleViewmodel)
    implementation(libs.lifecycleViewmodelCompose)

// EXOPLAYER & MEDIA SESSION
    implementation(libs.media3Exoplayer)
    implementation(libs.media3Ui)
    implementation(libs.media3Session)
    implementation(libs.media3Common)

// COROUTINES
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

// NAVIGATION
    implementation(libs.navigationFragment)
    implementation(libs.navigationUi)
    implementation(libs.navigationCompose)

// MATERIAL 3 (Jetpack Compose UI)
    implementation(libs.androidxComposeMaterial3)

// IMAGE LOADING
    implementation(libs.coilCompose)

}