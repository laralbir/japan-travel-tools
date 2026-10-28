import java.util.Properties
import java.io.FileInputStream

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

android {
  namespace = "com.laralbir.japantraveltools"
  compileSdk = 36
  buildToolsVersion = "36.1.0"
  defaultConfig {
    applicationId = "com.laralbir.japantraveltools"
    minSdk = 26
    targetSdk = 36
    versionCode = 35
    versionName = "1.2.0"
  }

  signingConfigs {
    create("release") {
      val keystorePropsFile = rootProject.file("keystore.properties")
      if (keystorePropsFile.exists()) {
        val properties = Properties().apply {
          FileInputStream(keystorePropsFile).use { stream ->
            load(stream)
          }
        }
        storeFile = file(properties.getProperty("storeFile") ?: "${rootProject.projectDir}/release.keystore")
        storePassword = properties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD") ?: "japantraveltools"
        keyAlias = properties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS") ?: "japantraveltools"
        keyPassword = properties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD") ?: "japantraveltools"
      } else {
        val defaultReleaseKeystore = rootProject.file("release.keystore")
        if (defaultReleaseKeystore.exists()) {
          storeFile = defaultReleaseKeystore
          storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "japantraveltools"
          keyAlias = System.getenv("KEY_ALIAS") ?: "japantraveltools"
          keyPassword = System.getenv("KEY_PASSWORD") ?: "japantraveltools"
        } else {
          val debugKeystore = file("${System.getProperty("user.home")}/.android/debug.keystore")
          if (debugKeystore.exists()) {
            storeFile = debugKeystore
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
          }
        }
      }
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    aidl = false
    buildConfig = true
    shaders = false
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

kotlin {
  jvmToolchain(17)
}

dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Real Biometrics & Device Credential Authentication
  implementation(libs.androidx.biometric)

  // ML Kit Japanese Text Recognition (OCR) for Camera Translation
  implementation(libs.mlkit.text.japanese)

  // WorkManager for Periodic Background Sync (Currency & Weather)
  implementation(libs.androidx.work.runtime.ktx)
  testImplementation(libs.androidx.work.testing)
}

tasks.register<Exec>("installAuthorizedDebug") {
  group = "install"
  description = "Builds and installs the Debug APK via ADB/Android CLI only on devices listed in authorized-devices.local.conf (git-ignored)."
  workingDir = rootProject.projectDir
  commandLine("sh", "${rootProject.projectDir}/scripts/deploy-authorized-devices.sh")
}
