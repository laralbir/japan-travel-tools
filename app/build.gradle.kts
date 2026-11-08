import java.io.FileInputStream
import java.util.Properties
import org.gradle.api.GradleException

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
    versionCode = 38
    versionName = "1.4.1"
  }

  signingConfigs {
    create("release") {
      val keystorePropsFile = rootProject.file("keystore.properties")
      val localProps = Properties().apply {
        if (keystorePropsFile.exists()) {
          FileInputStream(keystorePropsFile).use { load(it) }
        }
      }

      val resolvedStorePath = localProps.getProperty("storeFile")
        ?: System.getenv("SIGNING_KEYSTORE_PATH")
      val resolvedStorePassword = localProps.getProperty("storePassword")
        ?: System.getenv("SIGNING_STORE_PASSWORD")
      val resolvedKeyAlias = localProps.getProperty("keyAlias")
        ?: System.getenv("SIGNING_KEY_ALIAS")
      val resolvedKeyPassword = localProps.getProperty("keyPassword")
        ?: System.getenv("SIGNING_KEY_PASSWORD")

      if (!resolvedStorePath.isNullOrBlank() &&
          !resolvedStorePassword.isNullOrBlank() &&
          !resolvedKeyAlias.isNullOrBlank() &&
          !resolvedKeyPassword.isNullOrBlank()
      ) {
        storeFile = rootProject.file(resolvedStorePath)
        storePassword = resolvedStorePassword
        keyAlias = resolvedKeyAlias
        keyPassword = resolvedKeyPassword
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
      } else {
        val isReleaseTaskRequested = gradle.startParameter.taskNames.any { task ->
          task.contains("Release", ignoreCase = true)
        }
        if (System.getenv("CI") == "true" && isReleaseTaskRequested) {
          throw GradleException(
            "Missing release signing credentials in CI. Configure SIGNING_KEYSTORE_PATH, " +
              "SIGNING_STORE_PASSWORD, SIGNING_KEY_ALIAS, and SIGNING_KEY_PASSWORD."
          )
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

  testOptions {
    unitTests {
      isReturnDefaultValues = true
    }
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
  testImplementation("org.json:json:20240303")
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

  // ZXing for Offline QR, Aztec, PDF417 & Barcode Generation
  implementation(libs.zxing.core)
}

tasks.register<Exec>("installAuthorizedDebug") {
  group = "install"
  description = "Builds and installs the Debug APK via ADB/Android CLI only on devices listed in authorized-devices.local.conf (git-ignored)."
  workingDir = rootProject.projectDir
  commandLine("sh", "${rootProject.projectDir}/scripts/deploy-authorized-devices.sh")
}
