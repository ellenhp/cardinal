/*
 *    Copyright 2025 The Cardinal Authors
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

import com.android.build.api.dsl.ApkSigningConfig

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.cargo.ndk)
    kotlin("plugin.serialization") version "2.2.10"
}

android {
    namespace = "earth.maps.cardinal"
    compileSdk = 36

    defaultConfig {
        applicationId = "earth.maps.cardinal"
        minSdk = 26
        targetSdk = 36
        versionCode = System.getenv("VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = System.getenv("VERSION_NAME") ?: "debug"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "architecture"
    productFlavors {
        create("arm64") {
            dimension = "architecture"
            ndk {
                abiFilters += "arm64-v8a"
            }
            versionNameSuffix = "-arm64"
        }
        create("x86_64") {
            dimension = "architecture"
            ndk {
                abiFilters += "x86_64"
            }
            versionNameSuffix = "-x86_64"
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs["release"] as ApkSigningConfig
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    bundle {
        language {
            // Disable language splits for now to keep bundles simpler
            enableSplit = false
        }
        density {
            // Enable density splits for smaller downloads
            enableSplit = true
        }
        abi {
            // Enable ABI splits - this works with our product flavors
            enableSplit = true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }

    // Define a single UniFFI binding generation task outside of applicationVariants.all to avoid duplication
    val generateUniFFIBindings = tasks.register<Exec>("generateUniFFIBindings") {
        workingDir = file("../..")  // Workspace root directory
        commandLine = listOf(
            "cargo",
            "run",
            "--bin",
            "uniffi-bindgen",
            "-p",
            "cardinal-geocoder",
            "generate",
            "--library",
            "cardinal-android/app/src/main/jniLibs/arm64-v8a/libcardinal_geocoder.so",
            "--language",
            "kotlin",
            "--out-dir",
            layout.buildDirectory.dir("generated/source/uniffi/java").get().toString()
        )
        // Depend on both cargo builds to ensure native libraries are available
        dependsOn("buildCargoNdkArm64Release", "buildCargoNdkX86_64Release")
    }

    applicationVariants.all {
        val variant = this

        // Add dependency from Java compilation to the UniFFI binding generation task
        tasks.named("compile${variant.name.capitalize()}JavaWithJavac") {
            dependsOn(generateUniFFIBindings)
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
    module = ".."  // Point to workspace root directory
    librariesNames = arrayListOf("libcardinal_geocoder.so")
    buildType = "release"
    extraCargoBuildArguments = arrayListOf("-p", "cardinal-geocoder")
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.logging)
    implementation(libs.valhalla.mobile)
    implementation(libs.valhalla.models)
    implementation(libs.valhalla.config)
    implementation(libs.ferrostar.core)
    implementation(libs.ferrostar.maplibreui)
    implementation(libs.ferrostar.composeui)
    implementation(libs.okhttp3)
    implementation(libs.androidaddressformatter)

    // TODO: Migrate version to TOML (doesn't work). Likely related issue: https://github.com/gradle/gradle/issues/21267
    //noinspection UseTomlInstead
    implementation("net.java.dev.jna:jna:5.17.0@aar")

    coreLibraryDesugaring(libs.desugar.jdk.libs)

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
