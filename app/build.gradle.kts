import java.util.Properties

plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.plugin.compose"); id("com.google.devtools.ksp") }

android { namespace = "com.reza.khodro45monitor"; compileSdk = 35
 defaultConfig { applicationId = "com.reza.khodro45monitor"; minSdk = 26; targetSdk = 35; versionCode = 6; versionName = "1.1.1"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 signingConfigs {
  create("release") {
   val props = Properties().apply { load(rootProject.file("keystore.properties").inputStream()) }
   storeFile = rootProject.file(props.getProperty("storeFile"))
   storePassword = props.getProperty("storePassword")
   keyAlias = props.getProperty("keyAlias")
   keyPassword = props.getProperty("keyPassword")
  }
 }
 buildTypes { release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("release"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}

kotlin {
 jvmToolchain(17)
}
dependencies {
 implementation("androidx.core:core-ktx:1.15.0"); implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7"); implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
 implementation("androidx.activity:activity-compose:1.10.0"); implementation(platform("androidx.compose:compose-bom:2024.12.01")); implementation("androidx.compose.ui:ui"); implementation("androidx.compose.ui:ui-tooling-preview"); implementation("androidx.compose.material3:material3")
 implementation("androidx.room:room-runtime:2.6.1"); implementation("androidx.room:room-ktx:2.6.1"); ksp("androidx.room:room-compiler:2.6.1")
 implementation("androidx.datastore:datastore-preferences:1.1.1"); implementation("com.squareup.okhttp3:okhttp:4.12.0"); implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
 implementation("androidx.webkit:webkit:1.12.1")
 testImplementation("junit:junit:4.13.2"); testImplementation("org.json:json:20240303")
 debugImplementation("androidx.compose.ui:ui-tooling")
}
