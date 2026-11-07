package com.akerusan.fieldincidentreporter.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.akerusan.fieldincidentreporter.feature.incidentdetail.IncidentDetailRoute
import com.akerusan.fieldincidentreporter.feature.incidentdetail.navigation.IncidentDetailKey
import com.akerusan.fieldincidentreporter.feature.incidenteditor.IncidentEditorRoute
import com.akerusan.fieldincidentreporter.feature.incidenteditor.navigation.IncidentEditorKey
import com.akerusan.fieldincidentreporter.feature.incidentlist.IncidentListRoute
import com.akerusan.fieldincidentreporter.feature.incidentlist.navigation.IncidentListKey

/**
 * The app's navigation graph. Features don't know about each other: each exposes a key and a
 * composable with navigation callbacks, and this is the only place they are connected.
 *
 * The back stack is saved across configuration changes and process death. Each entry gets its own
 * ViewModel store, so a screen's ViewModel lives exactly as long as the screen is on the stack.
 */
@Composable
fun AppNavigation(modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(IncidentListKey)

    fun navigateBack() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = ::navigateBack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider<NavKey> {
            entry<IncidentListKey> {
                IncidentListRoute(
                    onIncidentClick = { id -> backStack.add(IncidentDetailKey(id)) },
                    onReportClick = { backStack.add(IncidentEditorKey()) },
                )
            }
            entry<IncidentDetailKey> { key ->
                IncidentDetailRoute(
                    incidentId = key.incidentId,
                    onBack = ::navigateBack,
                    onEdit = { id -> backStack.add(IncidentEditorKey(id)) },
                )
            }
            entry<IncidentEditorKey> { key ->
                IncidentEditorRoute(
                    incidentId = key.incidentId,
                    onClose = ::navigateBack,
                )
            }
        },
    )
}
