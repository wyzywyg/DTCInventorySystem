plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.dtcinventorysystem"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dtcinventorysystem"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX & UI
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // CardView for better UI
    implementation(libs.cardview)

    // CameraX
    implementation(libs.camera.core.v130)
    implementation(libs.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // ML Kit Barcode Scanner
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:common:18.11.0")

    // Guava for ListenableFuture
    implementation("com.google.guava:guava:32.1.3-android")

    // Firebase (optional — include only what you use, like Auth/Database)
    implementation(libs.firebase.auth.v2231)
    implementation(libs.firebase.database.v2030)
    implementation(libs.firebase.firestore)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation (libs.firebase.auth)
    implementation (libs.firebase.database)
}
