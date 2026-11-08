package com.akerusan.fieldincidentreporter.feature.incidenteditor

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.domain.usecase.AddPhotoUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.CreateCaptureTargetUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.DiscardDraftUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ObserveIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ReportIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.UpdateIncidentUseCase
import com.akerusan.fieldincidentreporter.core.domain.usecase.ValidateIncidentDraftUseCase
import com.akerusan.fieldincidentreporter.core.model.IncidentCategory
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentSeverity
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.model.PendingOperation
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.FakePhotoStore
import com.akerusan.fieldincidentreporter.core.testing.FixedIdGenerator
import com.akerusan.fieldincidentreporter.core.testing.MainDispatcherRule
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class IncidentEditorViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val savedPhoto = IncidentPhoto(id = "saved", localPath = "/photos/saved.jpg")
    private val repository = FakeIncidentRepository(listOf(testIncident(photos = listOf(savedPhoto))))
    private val photoStore = FakePhotoStore()

    private fun viewModel(
        incidentId: String? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): IncidentEditorViewModel {
        val validate = ValidateIncidentDraftUseCase()
        val clock = FakeClock()
        return IncidentEditorViewModel(
            incidentId = incidentId,
            savedStateHandle = savedStateHandle,
            observeIncident = ObserveIncidentUseCase(repository),
            reportIncident = ReportIncidentUseCase(repository, validate, FixedIdGenerator("incident"), clock),
            updateIncident = UpdateIncidentUseCase(repository, validate, photoStore, clock),
            validateDraft = validate,
            addPhoto = AddPhotoUseCase(photoStore, FixedIdGenerator("photo")),
            createCaptureTarget = CreateCaptureTargetUseCase(photoStore),
            discardDraft = DiscardDraftUseCase(photoStore),
        )
    }

    @Test
    fun `errors appear only after a save attempt, then update as the user types`() = runTest {
        val viewModel = viewModel()
        assertTrue(viewModel.uiState.value.errors.isEmpty())

        viewModel.onSave()
        assertEquals(
            setOf(DraftError.TITLE_MISSING, DraftError.CATEGORY_MISSING, DraftError.SEVERITY_MISSING),
            viewModel.uiState.value.errors,
        )

        viewModel.onTitleChange("Broken pump")
        assertEquals(setOf(DraftError.CATEGORY_MISSING, DraftError.SEVERITY_MISSING), viewModel.uiState.value.errors)
        assertTrue(repository.all.size == 1) // nothing new saved
    }

    @Test
    fun `a valid report is saved and the editor closes`() = runTest {
        val viewModel = viewModel()
        viewModel.fillValidDraft()

        viewModel.events.test {
            viewModel.onSave()
            assertEquals(IncidentEditorEvent.Close, awaitItem())
        }
        val saved = repository.all.single { it.id == "incident-1" }
        assertEquals("Broken pump", saved.title)
        assertEquals(PendingOperation.CREATE, saved.pendingOperation)
    }

    @Test
    fun `editing starts from the saved incident`() = runTest {
        val viewModel = viewModel(incidentId = "incident-1")

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Broken pump", state.draft.title)
        assertFalse(state.hasUnsavedChanges)
    }

    @Test
    fun `a resolved incident can't be opened for editing`() = runTest {
        repository.set(testIncident(status = IncidentStatus.RESOLVED))

        viewModel(incidentId = "incident-1").events.test {
            assertEquals(IncidentEditorEvent.ReadOnly, awaitItem())
            assertEquals(IncidentEditorEvent.Close, awaitItem())
        }
    }

    @Test
    fun `the draft survives process death`() = runTest {
        val savedStateHandle = SavedStateHandle()
        viewModel(savedStateHandle = savedStateHandle).onTitleChange("Half-typed report")

        val recreated = viewModel(savedStateHandle = savedStateHandle)

        assertEquals("Half-typed report", recreated.uiState.value.draft.title)
        assertTrue(recreated.uiState.value.hasUnsavedChanges)
    }

    @Test
    fun `a camera capture completes even if the app was recreated while the camera was open`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = viewModel(savedStateHandle = savedStateHandle)
        viewModel.events.test {
            viewModel.onTakePhoto()
            assertEquals(IncidentEditorEvent.LaunchCamera(photoStore.captureTarget), awaitItem())
        }

        val recreated = viewModel(savedStateHandle = savedStateHandle)
        recreated.onPhotoCaptured(success = true)

        assertEquals(listOf("photo-1"), recreated.uiState.value.draft.photos.map { it.id })
    }

    @Test
    fun `an unreadable image is reported and not attached`() = runTest {
        photoStore.unreadableUris += "content://picker/broken"
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onPhotosPicked(listOf("content://picker/broken"))
            assertEquals(IncidentEditorEvent.PhotoUnreadable, awaitItem())
        }
        assertTrue(viewModel.uiState.value.draft.photos.isEmpty())
    }

    @Test
    fun `removing a photo added in this session deletes its file, but a saved photo is kept until save`() = runTest {
        val viewModel = viewModel(incidentId = "incident-1")
        viewModel.onPhotosPicked(listOf("content://picker/new"))
        val added = viewModel.uiState.value.draft.photos.last()

        viewModel.onRemovePhoto(added)
        viewModel.onRemovePhoto(savedPhoto)

        assertEquals(listOf(added), photoStore.deletedPhotos)
        assertTrue(viewModel.uiState.value.draft.photos.isEmpty())
    }

    @Test
    fun `leaving with unsaved changes asks first, and discarding deletes the photos added`() = runTest {
        val viewModel = viewModel()
        viewModel.onPhotosPicked(listOf("content://picker/new"))

        viewModel.onBack()
        assertTrue(viewModel.uiState.value.showDiscardDialog)

        viewModel.events.test {
            viewModel.onDiscardConfirm()
            assertEquals(IncidentEditorEvent.Close, awaitItem())
        }
        assertEquals(listOf("photo-1"), photoStore.deletedPhotos.map { it.id })
    }

    @Test
    fun `leaving without changes closes right away`() = runTest {
        val viewModel = viewModel()

        viewModel.events.test {
            viewModel.onBack()
            assertEquals(IncidentEditorEvent.Close, awaitItem())
        }
    }

    private fun IncidentEditorViewModel.fillValidDraft() {
        onTitleChange("Broken pump")
        onCategorySelect(IncidentCategory.EQUIPMENT)
        onSeveritySelect(IncidentSeverity.HIGH)
    }
}
