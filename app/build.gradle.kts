plugins {
    alias(libs.plugins.android.application)
    kotlin("plugin.serialization")
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:2.1.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:gotrue-kt")

    implementation("io.ktor:ktor-client-android:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation(libs.material)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

android {
    namespace = "com.licoresluna2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.licoresluna2"
        minSdk = 24
        targetSdk = 27
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            optimization { enable = false }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}