plugins {
    alias(libs.plugins.fir.jvm.library)
}

dependencies {
    api(projects.core.domain)
    api(libs.junit)
    api(libs.kotlinx.coroutines.test)
    api(libs.turbine)
}
