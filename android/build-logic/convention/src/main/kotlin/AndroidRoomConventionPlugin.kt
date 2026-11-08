import androidx.room.gradle.RoomExtension
import com.akerusan.fieldincidentreporter.buildlogic.library
import com.akerusan.fieldincidentreporter.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room with KSP. The schema is exported to `schemas/` and checked in, so migrations can be reviewed. */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("androidx.room")
            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }
            dependencies {
                add("implementation", libs.library("androidx-room-runtime"))
                add("implementation", libs.library("androidx-room-ktx"))
                add("ksp", libs.library("androidx-room-compiler"))
                add("testImplementation", libs.library("androidx-room-testing"))
            }
        }
    }
}
