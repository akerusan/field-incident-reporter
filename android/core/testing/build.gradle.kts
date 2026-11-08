plugins {
    alias(libs.plugins.fir.android.library)
}

android {
    namespace = "com.akerusan.fieldincidentreporter.core.testing"
}

dependencies {
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
