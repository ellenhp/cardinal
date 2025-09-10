plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.cargo.ndk)
}

android {
    namespace = "earth.maps.cardinal"
    compileSdk = 36

    defaultConfig {
        applicationId = "earth.maps.cardinal"
        minSdk = 24
        targetSdk = 36
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
    buildFeatures {
        compose = true
    }

    applicationVariants.all {
        val variant = this
        val bDir = layout.buildDirectory.dir("generated/source/uniffi/${variant.name}/java").get()
        val generateBindings = tasks.register<Exec>("generate${variant.name.capitalize()}UniFFIBindings") {
             workingDir = file("../../cardinal-geocoder")
            commandLine = listOf("cargo", "run", "--bin", "uniffi-bindgen", "generate", "--library", "../cardinal-android/app/src/main/jniLibs/arm64-v8a/libcardinal_geocoder.so", "--language", "kotlin", "--out-dir", bDir.toString())
            
            dependsOn("buildCargoNdkRelease")
        }
        
        // Add dependency from Java compilation to generateBindings task
        tasks.named("compile${variant.name.capitalize()}JavaWithJavac") {
            dependsOn(generateBindings)
        }
    }
    
    sourceSets {
        getByName("main") {
            java.srcDir(layout.buildDirectory.dir("generated/source/uniffi"))
        }
    }
}

cargoNdk {
    targets = arrayListOf("arm64", "x86_64")
    module = "../cardinal-geocoder"  // Directory containing Cargo.toml
    librariesNames = arrayListOf("libcardinal_geocoder.so")
    buildType = "release"
}

dependencies {
    implementation(libs.maplibre.compose)
    implementation(libs.maplibre.compose.material3)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.logging)

    // TODO: Migrate version to TOML (doesn't work). Likely related issue: https://github.com/gradle/gradle/issues/21267
    //noinspection UseTomlInstead
    implementation("net.java.dev.jna:jna:5.17.0@aar")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.gson)
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
