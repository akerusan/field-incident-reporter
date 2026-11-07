package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.FixedIdGenerator
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReportIncidentUseCaseTest {
    private val repository = FakeIncidentRepository()
    private val clock = FakeClock()
    private val reportIncident = ReportIncidentUseCase(
        repository = repository,
        validateDraft = ValidateIncidentDraftUseCase(),
        idGenerator = FixedIdGenerator(),
        clock = clock,
    )

    @Test
    fun `a valid draft is saved locally as a pending create with a device-generated id`() = runTest {
        val result = reportIncident(testDraft(title = "  Broken pump  "))

        assertEquals(SaveIncidentResult.Saved("id-1"), result)
        val saved = repository.all.single()
        assertEquals("id-1", saved.id)
        assertEquals("Broken pump", saved.title)
        assertEquals(clock.now, saved.reportedAt)
        assertEquals(clock.now, saved.updatedAt)
        assertEquals(SyncState.PENDING, saved.syncState)
        assertEquals(PendingOperation.CREATE, saved.pendingOperation)
    }

    @Test
    fun `an invalid draft is rejected and nothing is saved`() = runTest {
        val result = reportIncident(testDraft(title = "", category = null))

        assertEquals(
            SaveIncidentResult.Invalid(setOf(DraftError.TITLE_MISSING, DraftError.CATEGORY_MISSING)),
            result,
        )
        assertTrue(repository.all.isEmpty())
    }
}
