package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.akerusan.fieldincidentreporter.core.designsystem.theme.FieldIncidentReporterTheme
import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IncidentEditorScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun `validation errors are shown next to their fields`() {
        setScreen(
            IncidentEditorUiState(
                isNewIncident = true,
                errors = setOf(DraftError.TITLE_MISSING, DraftError.CATEGORY_MISSING, DraftError.SEVERITY_MISSING),
            ),
        )

        composeRule.onNodeWithText("Give the incident a title").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a category").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a severity").assertIsDisplayed()
    }

    @Test
    fun `a new report has no status choice`() {
        setScreen(IncidentEditorUiState(isNewIncident = true))

        composeRule.onNodeWithText("Status").assertDoesNotExist()
        composeRule.onNodeWithText("Report incident").assertIsDisplayed()
    }

    @Test
    fun `an edit offers only the statuses a technician may set`() {
        setScreen(IncidentEditorUiState(isNewIncident = false, isLoading = false))

        // assertExists rather than assertIsDisplayed: the row may be below the test window's fold.
        composeRule.onNodeWithText("Open").assertExists()
        composeRule.onNodeWithText("In progress").assertExists()
        composeRule.onNodeWithText("Resolved").assertDoesNotExist()
    }

    @Test
    fun `tapping save saves`() {
        var saved = false
        setScreen(IncidentEditorUiState(isNewIncident = true), IncidentEditorActions(onSave = { saved = true }))

        composeRule.onNodeWithText("Save").performClick()

        assertTrue(saved)
    }

    @Test
    fun `save is disabled while saving`() {
        setScreen(IncidentEditorUiState(isNewIncident = true, isSaving = true))

        composeRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun `photo buttons are disabled once the photo limit is reached`() {
        val photos = List(6) { IncidentPhoto(id = "p$it") }
        setScreen(IncidentEditorUiState(isNewIncident = true, draft = IncidentDraft(photos = photos)))

        composeRule.onNodeWithText("Take photo").assertIsNotEnabled()
        composeRule.onNodeWithText("Choose photos").assertIsNotEnabled()
    }

    private fun setScreen(state: IncidentEditorUiState, actions: IncidentEditorActions = IncidentEditorActions()) {
        composeRule.setContent {
            FieldIncidentReporterTheme(dynamicColor = false) {
                IncidentEditorScreen(uiState = state, snackbarHostState = SnackbarHostState(), actions = actions)
            }
        }
    }
}
