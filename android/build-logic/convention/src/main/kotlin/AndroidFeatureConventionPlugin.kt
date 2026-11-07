import com.akerusan.fieldincidentreporter.buildlogic.library
import com.akerusan.fieldincidentreporter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * A feature module (`:feature:*`): Compose UI, Hilt ViewModels, and the modules a feature may
 * depend on. Features never depend on `:core:data`, `:core:database` or `:core:network`; they
 * reach data only through use cases in `:core:domain`.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("fir.android.library.compose")
            pluginManager.apply("fir.android.hilt")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                add("implementation", project(":core:model"))
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:designsystem"))

                add("implementation", libs.library("androidx-compose-material3"))
                add("implementation", libs.library("androidx-compose-material-icons-extended"))
                add("implementation", libs.library("androidx-hilt-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.library("androidx-lifecycle-viewmodel-savedstate"))
                add("implementation", libs.library("androidx-navigation3-runtime"))
                add("implementation", libs.library("kotlinx-serialization-json"))

                add("testImplementation", project(":core:testing"))
                add("testImplementation", libs.library("turbine"))
                add("testImplementation", libs.library("robolectric"))
                add("testImplementation", libs.library("androidx-compose-ui-test-junit4"))
                add("debugImplementation", libs.library("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
