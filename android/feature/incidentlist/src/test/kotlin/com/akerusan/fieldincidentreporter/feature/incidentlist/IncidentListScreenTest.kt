package com.akerusan.fieldincidentreporter.feature.incidentlist

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.akerusan.fieldincidentreporter.core.designsystem.preview.PreviewIncidents
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncidentListScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `pending and failed incidents show their sync state`() {
        setScreen(
            IncidentListUiState(
                isLoading = false,
                incidents = listOf(PreviewIncidents.pending, PreviewIncidents.failed),
            ),
        )

        composeRule.onNodeWithText("Waiting to sync").assertIsDisplayed()
        composeRule.onNodeWithText("Not synced").assertIsDisplayed()
    }

    @Test
    fun `the offline banner shows only when offline`() {
        setScreen(IncidentListUiState(isLoading = false, incidents = PreviewIncidents.all, isOnline = false))

        composeRule.onNodeWithText("You're offline", substring = true).assertIsDisplayed()
    }

    @Test
    fun `an empty filtered list says why it is empty`() {
        setScreen(IncidentListUiState(isLoading = false, statusFilter = IncidentStatus.RESOLVED))

        composeRule.onNodeWithText("No incidents have this status.").assertIsDisplayed()
    }

    @Test
    fun `tapping an incident opens it, and a filter chip selects its status`() {
        var opened: String? = null
        var filter: IncidentStatus? = null
        setScreen(
            IncidentListUiState(isLoading = false, incidents = listOf(PreviewIncidents.synced)),
            onIncidentClick = { opened = it },
            onStatusFilterSelected = { filter = it },
        )

        composeRule.onNodeWithText(PreviewIncidents.synced.title).performClick()
        composeRule.onNodeWithText("In progress").performClick()

        assertEquals(PreviewIncidents.synced.id, opened)
        assertEquals(IncidentStatus.IN_PROGRESS, filter)
    }

    private fun setScreen(
        state: IncidentListUiState,
        onIncidentClick: (String) -> Unit = {},
        onStatusFilterSelected: (IncidentStatus?) -> Unit = {},
    ) {
        composeRule.setContent {
            FieldIncidentReporterTheme(dynamicColor = false) {
                IncidentListScreen(
                    uiState = state,
                    snackbarHostState = SnackbarHostState(),
                    onIncidentClick = onIncidentClick,
                    onReportClick = {},
                    onStatusFilterSelected = onStatusFilterSelected,
                    onRefresh = {},
                )
            }
        }
    }
}
