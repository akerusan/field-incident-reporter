plugins {
    alias(libs.plugins.fir.android.library)
    alias(libs.plugins.fir.android.hilt)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.data"
}

dependencies {
    api(projects.core.domain)
    implementation(projects.core.database)
    implementation(projects.core.network)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.turbine)
}
