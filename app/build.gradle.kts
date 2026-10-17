plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.veloxsport.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.veloxsport.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("androidx.activity:activity-ktx:1.9.3")

    // Material Components untuk Theme.RideTrackPro
    implementation("com.google.android.material:material:1.12.0")
}