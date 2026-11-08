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
val apiBaseUrl: String = localProperties.getProperty("api.baseUrl") ?: "http://10.0.2.2:3000/"

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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)

    testImplementation(libs.okhttp.mockwebserver)
}
