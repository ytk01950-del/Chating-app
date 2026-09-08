import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  // alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Ensure environment variables from AI Studio container runtime (System.getenv)
// are synchronized into root .env file so the Secrets Gradle Plugin generates BuildConfig fields.
val rootEnvFile = rootProject.file(".env")
val existingEnvMap = mutableMapOf<String, String>()

if (rootEnvFile.exists()) {
  rootEnvFile.readLines().forEach { line ->
    val trimmed = line.trim()
    if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
      val parts = trimmed.split("=", limit = 2)
      if (parts.size == 2) {
        val key = parts[0].trim()
        val value = parts[1].trim()
        if (value.isNotEmpty()) {
          existingEnvMap[key] = value
        }
      }
    }
  }
}

fun resolveAnySecret(vararg keys: String): String {
  for (k in keys) {
    val envVal = System.getenv(k)
    if (!envVal.isNullOrBlank()) return envVal.trim()
    val propVal = project.findProperty(k) as? String
    if (!propVal.isNullOrBlank()) return propVal.trim()
    val fromMap = existingEnvMap[k]
    if (!fromMap.isNullOrBlank() && !fromMap.contains("placeholder") && !fromMap.contains("your-") && !fromMap.contains("dummy")) {
      return fromMap.trim()
    }
  }
  return ""
}

val resolvedSupabaseUrl = resolveAnySecret("SUPABASE_URL", "VITE_SUPABASE_URL", "NEXT_PUBLIC_SUPABASE_URL", "REACT_APP_SUPABASE_URL")
val resolvedSupabaseKey = resolveAnySecret(
  "SUPABASE_PUBLISHABLE_KEY",
  "SUPABASE_ANON_KEY",
  "SUPABASE_PUBLIC_KEY",
  "SUPABASE_KEY",
  "VITE_SUPABASE_ANON_KEY",
  "VITE_SUPABASE_PUBLISHABLE_KEY",
  "NEXT_PUBLIC_SUPABASE_ANON_KEY",
  "NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY"
)

if (resolvedSupabaseUrl.isNotEmpty()) {
  existingEnvMap["SUPABASE_URL"] = resolvedSupabaseUrl
}
if (resolvedSupabaseKey.isNotEmpty()) {
  existingEnvMap["SUPABASE_ANON_KEY"] = resolvedSupabaseKey
  existingEnvMap["SUPABASE_PUBLISHABLE_KEY"] = resolvedSupabaseKey
}

val geminiKey = resolveAnySecret("GEMINI_API_KEY", "VITE_GEMINI_API_KEY")
if (geminiKey.isNotEmpty()) {
  existingEnvMap["GEMINI_API_KEY"] = geminiKey
}

val envBuilder = StringBuilder()
existingEnvMap.forEach { (k, v) ->
  envBuilder.append("$k=$v\n")
}
rootEnvFile.writeText(envBuilder.toString())

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.livechat.rtcom"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    buildConfigField("String", "ENV_SUPABASE_URL", "\"${resolvedSupabaseUrl.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_KEY", "\"${resolvedSupabaseKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_ANON_KEY", "\"${resolvedSupabaseKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_PUBLISHABLE_KEY", "\"${resolvedSupabaseKey.replace("\"", "\\\"")}\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)
  // implementation(libs.firebase.ai)
  implementation(libs.firebase.auth)
  implementation(libs.firebase.database)
  // implementation(libs.firebase.storage) // Replaced with Supabase Storage $0 Free Tier
  implementation(libs.firebase.messaging)
  // Uncomment to use Firestore:
  // implementation(libs.firebase.firestore)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.firebase.appcheck.recaptcha)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  // "ksp"(libs.androidx.room.compiler)
  // "ksp"(libs.moshi.kotlin.codegen)
}
