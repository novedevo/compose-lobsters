plugins {
  kotlin("multiplatform")
  id("com.android.library")
  id("com.squareup.sqldelight") version "1.5.1"
}

kotlin {
  android()
  jvm("desktop") {
    compilations.all {
      kotlinOptions.jvmTarget = "11"
      kotlinOptions.freeCompilerArgs =
        kotlinOptions.freeCompilerArgs + listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
  }
  sourceSets {
    // Workaround for:
    //
    // The Kotlin source set androidAndroidTestRelease was configured but not added to any
    // Kotlin compilation. You can add a source set to a target's compilation by connecting it
    // with the compilation's default source set using 'dependsOn'.
    // See
    // https://kotlinlang.org/docs/reference/building-mpp-with-gradle.html#connecting-source-sets
    //
    // This workaround includes `dependsOn(androidAndroidTestRelease)` in the `androidTest`
    // sourceSet.
    val androidAndroidTestRelease by getting
    val commonMain by getting
    val commonTest by getting
    val androidMain by getting { dependencies { implementation(libs.sqldelight.androidDriver) } }
    val androidTest by getting { dependsOn(androidAndroidTestRelease) }
    val desktopMain by getting { dependencies { implementation(libs.sqldelight.jvmDriver) } }
    val desktopTest by getting {
      dependencies {
        implementation(libs.kotlin.coroutines.core)
        implementation(kotlin("test-junit"))
      }
    }
  }
}

android {
  compileSdk = 31
  sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
  defaultConfig {
    minSdk = 23
    targetSdk = 31
    consumerProguardFiles("consumer-rules.pro")
  }
}

configure<com.squareup.sqldelight.gradle.SqlDelightExtension> {
  database("LobstersDatabase") {
    packageName = "dev.msfjarvis.claw.database"
    sourceFolders = listOf("sqldelight")
  }
}
