package com.akerusan.fieldincidentreporter.feature.incidentlist

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.akerusan.fieldincidentreporter.core.domain.usecase.CommitPendingDeleteUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveConnectivityUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentsUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObservePendingDeletesUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveSyncActivityUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RefreshIncidentsUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RestoreIncidentUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.testing.FakeConnectivityObserver
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.FakeSyncMonitor
import com.akerusan.fieldincidentreporter.core.testing.FakeSyncScheduler
import com.akerusan.fieldincidentreporter.core.testing.MainDispatcherRule
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import java.time.Instant
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Dispatchers are unconfined, so each call below has finished updating [IncidentListViewModel.uiState]
 * by the time it returns; tests read the state directly rather than awaiting emissions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncidentListViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeIncidentRepository(
        listOf(
            testIncident(id = "open", reportedAt = Instant.parse("2026-11-07T08:00:00Z")),
            testIncident(
                id = "resolved",
                status = IncidentStatus.RESOLVED,
                reportedAt = Instant.parse("2026-11-06T08:00:00Z"),
            ),
        ),
    )
    private val connectivity = FakeConnectivityObserver(initial = true)
    private val syncMonitor = FakeSyncMonitor()
    private val syncScheduler = FakeSyncScheduler()
    private val undoUntil = Instant.parse("2026-11-07T09:00:30Z")

    // Lazy: created inside the test, after MainDispatcherRule has installed the test dispatcher.
    private val viewModel by lazy {
        IncidentListViewModel(
            savedStateHandle = SavedStateHandle(),
            observeIncidents = ObserveIncidentsUseCase(repository),
            observePendingDeletes = ObservePendingDeletesUseCase(repository),
            observeConnectivity = ObserveConnectivityUseCase(connectivity),
            observeSyncActivity = ObserveSyncActivityUseCase(syncMonitor),
            refreshIncidents = RefreshIncidentsUseCase(syncScheduler),
            restoreIncident = RestoreIncidentUseCase(repository),
            commitPendingDelete = CommitPendingDeleteUseCase(repository),
        )
    }

    private val state get() = viewModel.uiState.value

    /** `uiState` only runs while someone collects it, like the screen does. */
    private fun TestScope.collectUiState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun `shows incidents newest first, and filters by status`() = runTest {
        collectUiState()
        assertFalse(state.isLoading)
        assertEquals(listOf("open", "resolved"), state.incidents.map { it.id })

        viewModel.onStatusFilterSelected(IncidentStatus.RESOLVED)

        assertEquals(IncidentStatus.RESOLVED, state.statusFilter)
        assertEquals(listOf("resolved"), state.incidents.map { it.id })
    }

    @Test
    fun `the selected filter and the list are never out of step`() = runTest {
        val states = mutableListOf<IncidentListUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { states += it } }

        viewModel.onStatusFilterSelected(IncidentStatus.RESOLVED)
        viewModel.onStatusFilterSelected(IncidentStatus.OPEN)

        val content = states.filterNot { it.isLoading }
        assertTrue(content.size >= 3)
        assertTrue(
            content.all { state ->
                state.incidents.all {
                    state.statusFilter == null ||
                        it.status == state.statusFilter
                }
            },
        )
    }

    @Test
    fun `a deleted incident leaves the list and becomes the pending delete for the undo snackbar`() = runTest {
        collectUiState()
        repository.markDeleted("open", undoUntil)

        assertEquals(listOf("resolved"), state.incidents.map { it.id })
        assertEquals("open", state.pendingDelete?.id)

        viewModel.onUndoDelete("open")

        assertNull(state.pendingDelete)
        assertEquals(2, state.incidents.size)
    }

    @Test
    fun `dismissing the undo snackbar commits the delete`() = runTest {
        repository.markDeleted("open", undoUntil)

        viewModel.onUndoSnackbarDismissed("open")

        assertEquals(listOf("open"), repository.committedDeleteIds)
    }

    @Test
    fun `a newer delete commits the older one, so there is one undo at a time`() = runTest {
        collectUiState()
        repository.markDeleted("open", undoUntil)
        repository.markDeleted("resolved", undoUntil)

        assertEquals(1, repository.committedDeleteIds.size)
        assertTrue(state.pendingDelete != null)
    }

    @Test
    fun `undo after the delete reached the server explains it's too late`() = runTest {
        viewModel.messages.test {
            viewModel.onUndoDelete("open") // not pending: the fake reports TOO_LATE

            assertEquals(IncidentListMessage.ALREADY_DELETED, awaitItem())
        }
    }

    @Test
    fun `refreshing offline queues a sync and says so, without a spinner`() = runTest {
        collectUiState()
        connectivity.isOnline.value = false

        viewModel.messages.test {
            viewModel.onRefresh()

            assertEquals(IncidentListMessage.WILL_SYNC_WHEN_ONLINE, awaitItem())
        }
        assertEquals(1, syncScheduler.requestCount)
        assertFalse(state.isRefreshing)
        assertFalse(state.isOnline)
    }

    @Test
    fun `the refresh indicator stays on until the sync has run`() = runTest {
        collectUiState()

        viewModel.onRefresh()
        assertTrue(state.isRefreshing) // queued, not started yet

        syncMonitor.isSyncing.value = true
        assertTrue(state.isRefreshing)

        syncMonitor.isSyncing.value = false
        assertFalse(state.isRefreshing)
        assertEquals(1, syncScheduler.requestCount)
    }

    @Test
    fun `the refresh indicator stops even if a fast sync was never seen running`() = runTest {
        collectUiState()

        viewModel.onRefresh()
        advanceTimeBy(4.seconds)

        assertFalse(state.isRefreshing)
    }

    @Test
    fun `the refresh indicator gives up on a sync that never finishes`() = runTest {
        collectUiState()
        viewModel.onRefresh()
        syncMonitor.isSyncing.value = true

        advanceTimeBy(21.seconds)

        assertFalse(state.isRefreshing)
    }
}
