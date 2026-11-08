package com.akerusan.fieldincidentreporter.feature.incidentdetail

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.usecase.DeleteIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.DiscardLocalChangesUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RetrySyncUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.model.SyncState
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.MainDispatcherRule
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IncidentDetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIncidentRepository(listOf(testIncident()))

    private fun viewModel(id: String = "incident-1") = IncidentDetailViewModel(
        incidentId = id,
        observeIncident = ObserveIncidentUseCase(repository),
        deleteIncident = DeleteIncidentUseCase(repository, FakeClock()),
        retrySync = RetrySyncUseCase(repository),
        discardLocalChanges = DiscardLocalChangesUseCase(repository),
    )

    @Test
    fun `shows the incident, editable and deletable`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            val content = awaitContent()
            assertEquals("incident-1", content.incident.id)
            assertTrue(content.canModify)
        }
    }

    @Test
    fun `a resolved incident can be neither edited nor deleted`() = runTest {
        repository.set(testIncident(status = IncidentStatus.RESOLVED))

        viewModel().uiState.test {
            val content = awaitContent()
            assertFalse(content.canModify)
        }
    }

    @Test
    fun `a failed delete offers neither edit nor delete`() = runTest {
        repository.set(testIncident(syncState = SyncState.FAILED, pendingOperation = PendingOperation.DELETE))

        viewModel().uiState.test {
            val content = awaitContent()
            assertFalse(content.canModify)
        }
    }

    @Test
    fun `after deleting, the incident is gone and the screen can close`() = runTest {
        val viewModel = viewModel()

        viewModel.uiState.test {
            awaitContent()
            viewModel.onDeleteConfirmed()

            assertEquals(IncidentDetailUiState.Gone, awaitItem())
        }
    }

    @Test
    fun `an unknown incident is gone, not an error`() = runTest {
        viewModel(id = "unknown").uiState.test {
            var state = awaitItem()
            while (state == IncidentDetailUiState.Loading) state = awaitItem()
            assertEquals(IncidentDetailUiState.Gone, state)
        }
    }

    @Test
    fun `retry queues the change again`() = runTest {
        viewModel().onRetry()

        assertEquals(listOf("incident-1"), repository.retriedIds)
    }

    @Test
    fun `discarding without a connection explains why nothing happened`() = runTest {
        repository.discardResult = DiscardChangesResult.NEEDS_CONNECTION
        val viewModel = viewModel()

        viewModel.messages.test {
            viewModel.onDiscardChanges()

            assertEquals(IncidentDetailMessage.DISCARD_NEEDS_CONNECTION, awaitItem())
        }
    }

    @Test
    fun `a server error while discarding gets its own message`() = runTest {
        repository.discardResult = DiscardChangesResult.SERVER_ERROR
        val viewModel = viewModel()

        viewModel.messages.test {
            viewModel.onDiscardChanges()

            assertEquals(IncidentDetailMessage.DISCARD_SERVER_ERROR, awaitItem())
        }
    }

    private suspend fun ReceiveTurbine<IncidentDetailUiState>.awaitContent(): IncidentDetailUiState.Content {
        var state = awaitItem()
        while (state !is IncidentDetailUiState.Content) state = awaitItem()
        return state
    }
}
