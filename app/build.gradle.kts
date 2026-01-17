plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.angularprimer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.angularprimer"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Ship only the locales you care about (keep or change as needed)
        resourceConfigurations.add("en")
    }

    buildTypes {
        // Uncomment the block below if you want a smaller debug APK as well.
        // Expect slower rebuilds and less-friendly stack traces.
        /*
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = true
        }
        */

        release {
            // Shrink/obfuscate bytecode
            isMinifyEnabled = true
            // Remove unused resources after R8 runs
            isShrinkResources = true
            // Supply your own signingConfig here or via Gradle properties
            // signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Basic packaging cleanup (optional but saves a bit of space)
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1,LICENSE*,NOTICE*}"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.activity:activity-compose:1.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}