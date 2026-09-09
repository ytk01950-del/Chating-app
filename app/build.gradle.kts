import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  // alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

// Comprehensive secret resolver for AI Studio environment variables, Gradle properties, and .env files
val rootEnvFile = rootProject.file(".env")
val appEnvFile = project.file(".env")
val existingEnvMap = mutableMapOf<String, String>()

listOf(rootEnvFile, appEnvFile).forEach { envFile ->
  if (envFile.exists()) {
    try {
      envFile.readLines().forEach { line ->
        val trimmed = line.trim()
        if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
          val parts = trimmed.split("=", limit = 2)
          if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            if (value.isNotEmpty() && !existingEnvMap.containsKey(key)) {
              existingEnvMap[key] = value
            }
          }
        }
      }
    } catch (e: Exception) {
      // Ignored
    }
  }
}

fun isPlaceholder(v: String?): Boolean {
  if (v.isNullOrBlank()) return true
  val t = v.trim().lowercase()
  return t == "null" || t == "none" || t.contains("your-") || t.contains("example.com") || t.contains("placeholder") || t.contains("dummy")
}

fun resolveAnySecret(vararg keys: String): String {
  for (k in keys) {
    // 1. Direct System.getenv
    val envVal = System.getenv(k)
    if (!isPlaceholder(envVal)) return envVal!!.trim()
    
    // 2. Case-insensitive System.getenv search
    val caseMatch = System.getenv().entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value
    if (!isPlaceholder(caseMatch)) return caseMatch!!.trim()

    // 3. System properties
    val sysProp = System.getProperty(k)
    if (!isPlaceholder(sysProp)) return sysProp!!.trim()

    // 4. Gradle project property
    val propVal = (project.findProperty(k) ?: rootProject.findProperty(k)) as? String
    if (!isPlaceholder(propVal)) return propVal!!.trim()

    // 5. Existing .env map
    val fromMap = existingEnvMap[k]
    if (!isPlaceholder(fromMap)) return fromMap!!.trim()
  }
  return ""
}

// 1. Supabase Project URL
val resolvedSupabaseUrl = resolveAnySecret(
  "SUPABASE_URL",
  "VITE_SUPABASE_URL",
  "NEXT_PUBLIC_SUPABASE_URL",
  "REACT_APP_SUPABASE_URL",
  "SUPABASE_PROJECT_URL"
)

// 2. Supabase Publishable Key (Preferred)
val resolvedSupabasePublishableKey = resolveAnySecret(
  "SUPABASE_PUBLISHABLE_KEY",
  "VITE_SUPABASE_PUBLISHABLE_KEY",
  "NEXT_PUBLIC_SUPABASE_PUBLISHABLE_KEY"
)

// 3. Supabase Anon Key (Fallback)
val resolvedSupabaseAnonKey = resolveAnySecret(
  "SUPABASE_ANON_KEY",
  "SUPABASE_PUBLIC_KEY",
  "SUPABASE_KEY",
  "VITE_SUPABASE_ANON_KEY",
  "NEXT_PUBLIC_SUPABASE_ANON_KEY"
)

// Effective public key: Prefer publishable key, fall back to anon key
val resolvedSupabaseEffectiveKey = if (resolvedSupabasePublishableKey.isNotBlank()) {
  resolvedSupabasePublishableKey
} else {
  resolvedSupabaseAnonKey
}

val geminiKey = resolveAnySecret("GEMINI_API_KEY", "VITE_GEMINI_API_KEY")

// Write back resolved secrets to root .env if missing so other plugins have access
try {
  if (resolvedSupabaseUrl.isNotBlank()) existingEnvMap["SUPABASE_URL"] = resolvedSupabaseUrl
  if (resolvedSupabasePublishableKey.isNotBlank()) existingEnvMap["SUPABASE_PUBLISHABLE_KEY"] = resolvedSupabasePublishableKey
  if (resolvedSupabaseAnonKey.isNotBlank()) existingEnvMap["SUPABASE_ANON_KEY"] = resolvedSupabaseAnonKey
  if (geminiKey.isNotBlank()) existingEnvMap["GEMINI_API_KEY"] = geminiKey

  if (existingEnvMap.isNotEmpty()) {
    val envContent = existingEnvMap.entries.joinToString("\n") { "${it.key}=${it.value}" } + "\n"
    rootEnvFile.writeText(envContent)
  }
} catch (e: Exception) {
  // Ignored in read-only setups
}

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

    buildConfigField("String", "SUPABASE_URL", "\"${resolvedSupabaseUrl.replace("\"", "\\\"")}\"")
    buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${resolvedSupabasePublishableKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"${resolvedSupabaseAnonKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_URL", "\"${resolvedSupabaseUrl.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_KEY", "\"${resolvedSupabaseEffectiveKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_ANON_KEY", "\"${resolvedSupabaseAnonKey.replace("\"", "\\\"")}\"")
    buildConfigField("String", "ENV_SUPABASE_PUBLISHABLE_KEY", "\"${resolvedSupabasePublishableKey.replace("\"", "\\\"")}\"")
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
  ignoreList.add("SUPABASE_URL")
  ignoreList.add("SUPABASE_ANON_KEY")
  ignoreList.add("SUPABASE_PUBLISHABLE_KEY")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.core)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
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
