plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(project(":composeApp"))
            implementation("androidx.activity:activity-compose:1.8.2")
            implementation("io.insert-koin:koin-android:3.5.3")
        }
    }
}

android {
    namespace = "com.studymate.android"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.studymate.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        // Read API key from local.properties
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
            buildConfigField("String", "GEMINI_API_KEY", "\"${properties.getProperty("gemini.api.key", "")}\"")
        } else {
            buildConfigField("String", "GEMINI_API_KEY", "\"\"")
        }
    }
    
    buildFeatures {
        buildConfig = true
        compose = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
