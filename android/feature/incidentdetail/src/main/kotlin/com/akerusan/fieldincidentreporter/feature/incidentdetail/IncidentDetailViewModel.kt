package com.akerusan.fieldincidentreporter.feature.incidentdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.akerusan.fieldincidentreporter.core.domain.result.DeleteIncidentResult
import com.akerusan.fieldincidentreporter.core.domain.result.DiscardChangesResult
import com.akerusan.fieldincidentreporter.core.domain.usecase.DeleteIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.DiscardLocalChangesUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.RetrySyncUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = IncidentDetailViewModel.Factory::class)
class IncidentDetailViewModel @AssistedInject constructor(
    @Assisted private val incidentId: String,
    observeIncident: ObserveIncidentUseCase,
    private val deleteIncident: DeleteIncidentUseCase,
    private val retrySync: RetrySyncUseCase,
    private val discardLocalChanges: DiscardLocalChangesUseCase,
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(incidentId: String): IncidentDetailViewModel
    }

    private val isDiscarding = MutableStateFlow(false)

    private val messageChannel = Channel<IncidentDetailMessage>(Channel.BUFFERED)
    val messages: Flow<IncidentDetailMessage> = messageChannel.receiveAsFlow()

    val uiState: StateFlow<IncidentDetailUiState> = combine(
        observeIncident(incidentId),
        isDiscarding,
    ) { incident, discarding ->
        if (incident == null) {
            IncidentDetailUiState.Gone
        } else {
            IncidentDetailUiState.Content(incident, isDiscarding = discarding)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), IncidentDetailUiState.Loading)

    /** Once deleted, the incident is hidden, the state becomes [IncidentDetailUiState.Gone] and the screen closes. */
    fun onDeleteConfirmed() {
        viewModelScope.launch {
            if (deleteIncident(incidentId) == DeleteIncidentResult.ReadOnly) {
                messageChannel.send(IncidentDetailMessage.CANNOT_DELETE_RESOLVED)
            }
        }
    }

    fun onRetry() {
        viewModelScope.launch { retrySync(incidentId) }
    }

    fun onDiscardChanges() {
        if (isDiscarding.value) return
        viewModelScope.launch {
            isDiscarding.value = true
            val result = try {
                discardLocalChanges(incidentId)
            } finally {
                isDiscarding.value = false
            }
            when (result) {
                DiscardChangesResult.DISCARDED -> messageChannel.send(IncidentDetailMessage.CHANGES_DISCARDED)

                DiscardChangesResult.NEEDS_CONNECTION -> messageChannel.send(
                    IncidentDetailMessage.DISCARD_NEEDS_CONNECTION,
                )

                DiscardChangesResult.NOTHING_TO_DISCARD -> Unit
            }
        }
    }
}
