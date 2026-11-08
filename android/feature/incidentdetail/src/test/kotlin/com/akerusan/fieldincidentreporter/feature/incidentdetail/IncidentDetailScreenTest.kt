package com.akerusan.fieldincidentreporter.feature.incidentdetail

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.akerusan.fieldincidentreporter.core.designsystem.preview.PreviewIncidents
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.model.Incident
import com.akerusan.fieldincidentreporter.core.model.SyncError
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncidentDetailScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `a resolved incident is read-only and says why`() {
        setScreen(PreviewIncidents.resolved)

        composeRule.onNodeWithContentDescription("Edit").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Delete").assertDoesNotExist()
        composeRule.onNodeWithText("Resolved by an operator", substring = true).assertIsDisplayed()
    }

    @Test
    fun `an open incident can be edited and deleted`() {
        setScreen(PreviewIncidents.synced)

        composeRule.onNodeWithContentDescription("Edit").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Delete").assertIsDisplayed()
    }

    @Test
    fun `a change refused because an operator resolved the incident leads with discard`() {
        var discarded = false
        setScreen(PreviewIncidents.failed, onDiscardChanges = { discarded = true })

        composeRule.onNodeWithText("An operator resolved this incident", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Retry").assertDoesNotExist()
        composeRule.onNodeWithText("Discard my changes").performClick()

        assertTrue(discarded)
    }

    @Test
    fun `a rejected change can be retried`() {
        var retried = false
        setScreen(
            PreviewIncidents.failed.copy(
                syncError = SyncError(SyncError.Reason.REJECTED_BY_SERVER, "title is required"),
            ),
            onRetry = { retried = true },
        )

        composeRule.onNodeWithText("The server rejected this change: title is required").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()

        assertTrue(retried)
    }

    @Test
    fun `deleting asks for confirmation first`() {
        var deleted = false
        setScreen(PreviewIncidents.synced, onDeleteConfirmed = { deleted = true })

        composeRule.onNodeWithContentDescription("Delete").performClick()
        composeRule.onNodeWithText("Delete this incident?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        assertTrue(deleted)
    }

    private fun setScreen(
        incident: Incident,
        onDeleteConfirmed: () -> Unit = {},
        onRetry: () -> Unit = {},
        onDiscardChanges: () -> Unit = {},
    ) {
        composeRule.setContent {
            FieldIncidentReporterTheme(dynamicColor = false) {
                IncidentDetailScreen(
                    uiState = IncidentDetailUiState.Content(incident),
                    snackbarHostState = SnackbarHostState(),
                    onBack = {},
                    onEdit = {},
                    onDeleteConfirmed = onDeleteConfirmed,
                    onRetry = onRetry,
                    onDiscardChanges = onDiscardChanges,
                )
            }
        }
    }
}
