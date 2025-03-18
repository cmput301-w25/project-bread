import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.bread"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bread"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }

        val firebaseProjectId = localProperties.getProperty("FIREBASE_PROJECT_ID")

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${firebaseProjectId}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.geofire.android.common)
    implementation(libs.firebase.auth)

    // Google Services
    implementation(libs.play.services.location)
    implementation(libs.legacy.support.v4)

    // testing
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // mockito
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.inline)
    implementation(libs.byte.buddy)

    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
