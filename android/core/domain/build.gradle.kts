plugins {
    alias(libs.plugins.fir.jvm.library)
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}

dependencies {
    testImplementation(projects.core.testing)
}
