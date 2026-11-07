package com.akerusan.fieldincidentreporter.core.domain.usecase

import app.cash.turbine.test
import com.akerusan.fieldincidentreporter.core.domain.result.DeleteIncidentResult
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteIncidentUseCaseTest {
    private val repository = FakeIncidentRepository(listOf(testIncident()))
    private val deleteIncident = DeleteIncidentUseCase(repository, FakeClock())

    @Test
    fun `a deleted incident is hidden and can be restored`() = runTest {
        assertEquals(DeleteIncidentResult.Deleted, deleteIncident("incident-1"))
        assertTrue(ObserveIncidentsUseCase(repository)(IncidentFilter()).first().isEmpty())

        ObservePendingDeletesUseCase(repository)().test {
            assertEquals(listOf("incident-1"), awaitItem().map { it.id })
            assertEquals(RestoreResult.RESTORED, RestoreIncidentUseCase(repository)("incident-1"))
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test
    fun `a resolved incident can't be deleted`() = runTest {
        repository.set(testIncident(status = IncidentStatus.RESOLVED))

        assertEquals(DeleteIncidentResult.ReadOnly, deleteIncident("incident-1"))
    }

    @Test
    fun `deleting an incident that is already being deleted is a no-op`() = runTest {
        deleteIncident("incident-1")

        assertEquals(DeleteIncidentResult.Deleted, deleteIncident("incident-1"))
    }

    @Test
    fun `restoring an incident that is already gone is too late`() = runTest {
        assertEquals(RestoreResult.TOO_LATE, RestoreIncidentUseCase(repository)("unknown"))
    }
}
