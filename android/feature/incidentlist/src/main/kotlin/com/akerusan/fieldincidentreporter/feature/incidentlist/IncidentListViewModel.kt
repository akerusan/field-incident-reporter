package com.akerusan.fieldincidentreporter.feature.incidentlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akerusan.fieldincidentreporter.core.domain.result.RestoreResult
import com.akerusan.fieldincidentreporter.core.domain.usecase.CommitPendingDeleteUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveConnectivityUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentsUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObservePendingDeletesUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveSyncActivityUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RefreshIncidentsUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RestoreIncidentUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentFilter
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class IncidentListViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    observeIncidents: ObserveIncidentsUseCase,
    observePendingDeletes: ObservePendingDeletesUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
    private val observeSyncActivity: ObserveSyncActivityUseCase,
    private val refreshIncidents: RefreshIncidentsUseCase,
    private val restoreIncident: RestoreIncidentUseCase,
    private val commitPendingDelete: CommitPendingDeleteUseCase,
) : ViewModel() {

    private val statusFilter = savedStateHandle.getStateFlow<IncidentStatus?>(KEY_STATUS_FILTER, null)
    private val isRefreshing = MutableStateFlow(false)
    private var refreshJob: Job? = null

    private val messageChannel = Channel<IncidentListMessage>(Channel.BUFFERED)
    val messages: Flow<IncidentListMessage> = messageChannel.receiveAsFlow()

    // The filter travels with its results, so the selected chip and the list never disagree.
    private val filteredIncidents = statusFilter.flatMapLatest { status ->
        observeIncidents(IncidentFilter(status = status)).map { incidents -> status to incidents }
    }

    val uiState: StateFlow<IncidentListUiState> = combine(
        filteredIncidents,
        observePendingDeletes(),
        observeConnectivity(),
        isRefreshing,
    ) { (filter, incidents), pendingDeletes, isOnline, refreshing ->
        IncidentListUiState(
            isLoading = false,
            incidents = incidents,
            statusFilter = filter,
            isOnline = isOnline,
            isRefreshing = refreshing,
            pendingDelete = pendingDeletes.firstOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncidentListUiState())

    fun onStatusFilterSelected(status: IncidentStatus?) {
        savedStateHandle[KEY_STATUS_FILTER] = status
    }

    /**
     * Pull-to-refresh. The indicator stays on until the sync has run, rather than flickering off
     * while the work is queued.
     */
    fun onRefresh() {
        if (refreshJob?.isActive == true) return
        refreshJob = viewModelScope.launch {
            refreshIncidents()
            if (!uiState.value.isOnline) {
                messageChannel.send(IncidentListMessage.WILL_SYNC_WHEN_ONLINE)
                return@launch
            }
            isRefreshing.value = true
            try {
                // A fast sync can be queued and done before we ever see it, so the wait for it to
                // appear is short; the wait for it to finish is bounded too.
                withTimeoutOrNull(SYNC_START_TIMEOUT) { observeSyncActivity().first { it } }
                withTimeoutOrNull(SYNC_FINISH_TIMEOUT) { observeSyncActivity().first { !it } }
            } finally {
                isRefreshing.value = false
            }
        }
    }

    fun onUndoDelete(incidentId: String) {
        viewModelScope.launch {
            if (restoreIncident(incidentId) == RestoreResult.TOO_LATE) {
                messageChannel.send(IncidentListMessage.ALREADY_DELETED)
            }
        }
    }

    /** The undo snackbar timed out or was swiped away: send the delete now. */
    fun onUndoSnackbarDismissed(incidentId: String) {
        viewModelScope.launch { commitPendingDelete(incidentId) }
    }

    private companion object {
        const val KEY_STATUS_FILTER = "statusFilter"
        val SYNC_START_TIMEOUT = 3.seconds
        val SYNC_FINISH_TIMEOUT = 20.seconds
    }
}
