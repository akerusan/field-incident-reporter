import java.util.Properties

plugins {
    alias(libs.plugins.fir.android.library)
    alias(libs.plugins.fir.android.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// The mock API base URL. Defaults to the host machine as seen from the Android emulator; a
// physical device can override it with `api.baseUrl=http://<host>:3000/` in local.properties.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}
// Retrofit requires a trailing slash, so one is added if the override leaves it out.
val apiBaseUrl: String = (localProperties.getProperty("api.baseUrl") ?: "http://10.0.2.2:3000/").trimEnd('/') + "/"

android {
    namespace = "com.akerusan.fieldincidentreporter.core.network"
    buildFeatures {
        buildConfig = true
    }
    defaultConfig {
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }
}

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.retrofit)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit.kotlinx.serialization)
}
