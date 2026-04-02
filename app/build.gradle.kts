val appName: String = "DebugHelper"
val pkgName: String = "top.kmiit.debughelper"
val verCode: Int = getVersionCode()
val verName: String = "0.0.1"

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.rust)
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.foundation)
    implementation(libs.miuix.android)
    implementation(libs.hiddenapibypass)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.miuix.icons.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.google.guava)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.miuix.navigation3.ui)
}

android {
    namespace = pkgName

    defaultConfig {
        applicationId = pkgName
        minSdk = 33
        versionCode = verCode
        versionName = verName
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

    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    ndkVersion = "29.0.14206865"
}

base {
    archivesName.set(
        "${appName}-v${verName}(${verCode})"
    )
}

androidRust {
    module("dh") {
        path = file("src/main/rust/dh")
        targets = listOf("arm64", "x86_64")
    }
}

kotlin {
    jvmToolchain(24)
}

fun getGitCommitCount(): Int {
    val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
    return process.inputStream.bufferedReader().use { it.readText().trim().toInt() }
}

fun getVersionCode(): Int = getGitCommitCount()
