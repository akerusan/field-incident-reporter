package com.akerusan.fieldincidentreporter.feature.incidentlist

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.akerusan.fieldincidentreporter.core.designsystem.component.EmptyState
import com.akerusan.fieldincidentreporter.core.designsystem.component.OfflineBanner
import com.akerusan.fieldincidentreporter.core.designsystem.label.labelRes
import com.akerusan.fieldincidentreporter.core.designsystem.preview.PreviewIncidents
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.designsystem.util.ObserveAsEvents
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus

/** The incident list with its ViewModel. [onIncidentClick] and [onReportClick] navigate. */
@Composable
fun IncidentListRoute(
    onIncidentClick: (incidentId: String) -> Unit,
    onReportClick: () -> Unit,
    viewModel: IncidentListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    ObserveAsEvents(viewModel.messages) { message ->
        val text = when (message) {
            IncidentListMessage.ALREADY_DELETED -> R.string.incident_list_already_deleted
            IncidentListMessage.WILL_SYNC_WHEN_ONLINE -> R.string.incident_list_will_sync
        }
        snackbarHostState.showSnackbar(resources.getString(text))
    }

    // The undo snackbar follows the state: it shows while a delete can be undone. If the screen
    // leaves composition, showSnackbar is cancelled, which is not a dismissal: nothing is
    // committed, and the snackbar shows again when the screen comes back.
    val pendingDeleteId = uiState.pendingDelete?.id
    LaunchedEffect(pendingDeleteId) {
        if (pendingDeleteId == null) return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = resources.getString(R.string.incident_list_deleted),
            actionLabel = resources.getString(R.string.incident_list_undo),
            withDismissAction = true,
            duration = SnackbarDuration.Long,
        )
        when (result) {
            SnackbarResult.ActionPerformed -> viewModel.onUndoDelete(pendingDeleteId)
            SnackbarResult.Dismissed -> viewModel.onUndoSnackbarDismissed(pendingDeleteId)
        }
    }

    IncidentListScreen(
        uiState = uiState,
        snackbarHostState = snackbarHostState,
        onIncidentClick = onIncidentClick,
        onReportClick = onReportClick,
        onStatusFilterSelected = viewModel::onStatusFilterSelected,
        onRefresh = viewModel::onRefresh,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IncidentListScreen(
    uiState: IncidentListUiState,
    snackbarHostState: SnackbarHostState,
    onIncidentClick: (String) -> Unit,
    onReportClick: () -> Unit,
    onStatusFilterSelected: (IncidentStatus?) -> Unit,
    onRefresh: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.incident_list_title)) }) },
        floatingActionButton = {
            val reportLabel = stringResource(R.string.incident_list_report)
            ExtendedFloatingActionButton(
                onClick = onReportClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text(reportLabel) },
                // The button's text slot isn't exposed to accessibility services, so label it here.
                modifier = Modifier.semantics { contentDescription = reportLabel },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            OfflineBanner(visible = !uiState.isOnline)
            StatusFilterRow(selected = uiState.statusFilter, onSelected = onStatusFilterSelected)
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    uiState.incidents.isEmpty() -> EmptyList(isFiltered = uiState.statusFilter != null)
                    else -> IncidentList(incidents = uiState.incidents, onIncidentClick = onIncidentClick)
                }
            }
        }
    }
}

@Composable
private fun StatusFilterRow(selected: IncidentStatus?, onSelected: (IncidentStatus?) -> Unit) {
    val description = stringResource(R.string.incident_list_filters)
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.FilterList, contentDescription = null)
        FilterChip(
            selected = selected == null,
            onClick = { onSelected(null) },
            label = { Text(stringResource(R.string.incident_list_filter_all)) },
        )
        IncidentStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelected(status) },
                label = { Text(stringResource(status.labelRes)) },
            )
        }
    }
}

@Composable
private fun IncidentList(incidents: List<Incident>, onIncidentClick: (String) -> Unit) {
    val listState = rememberLazyListState()
    // A lazy list keeps its first visible item in place, so an incident added at the top (a new
    // report, or one pulled from the server) would land just above the viewport. When the user is
    // at the top, ask the next measure (the one with the new item) to stay at index 0 instead.
    val firstIncidentId = incidents.firstOrNull()?.id
    val previousFirstIncidentId = remember { mutableStateOf(firstIncidentId) }
    SideEffect {
        if (firstIncidentId != previousFirstIncidentId.value) {
            if (listState.firstVisibleItemIndex == 0 && !listState.isScrollInProgress) {
                listState.requestScrollToItem(0)
            }
            previousFirstIncidentId.value = firstIncidentId
        }
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        // Bottom padding keeps the last card clear of the floating action button.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(incidents, key = { it.id }) { incident ->
            IncidentCard(
                incident = incident,
                onClick = { onIncidentClick(incident.id) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun EmptyList(isFiltered: Boolean) {
    // Scrollable so pull-to-refresh works on an empty list too.
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(top = 64.dp)) {
        item {
            Box(modifier = Modifier.fillParentMaxWidth(), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.AutoMirrored.Outlined.Assignment,
                    title = stringResource(
                        if (isFiltered) {
                            R.string.incident_list_empty_filtered_title
                        } else {
                            R.string.incident_list_empty_title
                        },
                    ),
                    message = stringResource(
                        if (isFiltered) {
                            R.string.incident_list_empty_filtered_message
                        } else {
                            R.string.incident_list_empty_message
                        },
                    ),
                )
            }
        }
    }
}

@Preview(name = "Content")
@Preview(name = "Content, dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun IncidentListScreenPreview() {
    FieldIncidentReporterTheme(dynamicColor = false) {
        IncidentListScreen(
            uiState = IncidentListUiState(isLoading = false, incidents = PreviewIncidents.all, isOnline = false),
            snackbarHostState = SnackbarHostState(),
            onIncidentClick = {},
            onReportClick = {},
            onStatusFilterSelected = {},
            onRefresh = {},
        )
    }
}

@Preview(name = "Empty")
@Composable
private fun IncidentListScreenEmptyPreview() {
    FieldIncidentReporterTheme(dynamicColor = false) {
        IncidentListScreen(
            uiState = IncidentListUiState(isLoading = false),
            snackbarHostState = SnackbarHostState(),
            onIncidentClick = {},
            onReportClick = {},
            onStatusFilterSelected = {},
            onRefresh = {},
        )
    }
}
