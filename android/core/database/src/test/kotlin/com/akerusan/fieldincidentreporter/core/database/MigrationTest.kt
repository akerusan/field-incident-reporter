package com.akerusan.fieldincidentreporter.core.database

import android.content.Context
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Every schema change must migrate data already on devices; this checks each step. */
@RunWith(RobolectricTestRunner::class)
class MigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        instrumentation = InstrumentationRegistry.getInstrumentation(),
        file = ApplicationProvider.getApplicationContext<Context>().getDatabasePath("migration-test.db"),
        driver = AndroidSQLiteDriver(),
        databaseClass = FirDatabase::class,
    )

    @Test
    fun `1 to 2 keeps existing incidents and starts their server-error count at 0`() {
        helper.createDatabase(1).use { connection ->
            connection.execSQL(
                """
                INSERT INTO incidents (id, title, description, category, severity, status, location_note,
                    reported_at, updated_at, sync_state, pending_operation, local_version, synced_version)
                VALUES ('incident-1', 'Broken pump', '', 'EQUIPMENT', 'HIGH', 'OPEN', '',
                    0, 0, 'PENDING', 'UPDATE', 2, 1)
                """.trimIndent(),
            )
        }

        helper.runMigrationsAndValidate(2).use { connection ->
            connection.prepare("SELECT title, server_error_count FROM incidents WHERE id = 'incident-1'").use {
                it.step()
                assertEquals("Broken pump", it.getText(0))
                assertEquals(0L, it.getLong(1))
            }
        }
    }
}
