package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.domain.result.SaveIncidentResult
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakeIncidentRepository
import com.akerusan.fieldincidentreporter.core.testing.FakePhotoStore
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import com.akerusan.fieldincidentreporter.core.testing.testIncident
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateIncidentUseCaseTest {
    private val keptPhoto = IncidentPhoto(id = "kept", localPath = "/photos/kept.jpg")
    private val removedPhoto = IncidentPhoto(id = "removed", localPath = "/photos/removed.jpg")
    private val repository = FakeIncidentRepository(listOf(testIncident(photos = listOf(keptPhoto, removedPhoto))))
    private val photoStore = FakePhotoStore()
    private val clock = FakeClock()
    private val updateIncident = UpdateIncidentUseCase(repository, ValidateIncidentDraftUseCase(), photoStore, clock)

    @Test
    fun `saves the change and deletes the files of removed photos`() = runTest {
        val result = updateIncident("incident-1", testDraft(title = "Pump replaced", photos = listOf(keptPhoto)))

        assertEquals(SaveIncidentResult.Saved("incident-1"), result)
        val saved = repository.all.single()
        assertEquals("Pump replaced", saved.title)
        assertEquals(listOf(keptPhoto), saved.photos)
        assertEquals(clock.now, saved.updatedAt)
        assertEquals(listOf(removedPhoto), photoStore.deletedPhotos)
    }

    @Test
    fun `a resolved incident can't be changed`() = runTest {
        repository.set(testIncident(status = IncidentStatus.RESOLVED))

        assertEquals(SaveIncidentResult.ReadOnly, updateIncident("incident-1", testDraft()))
    }

    @Test
    fun `an invalid draft is rejected without touching photos`() = runTest {
        val result = updateIncident("incident-1", testDraft(title = "", photos = emptyList()))

        assertEquals(SaveIncidentResult.Invalid(setOf(DraftError.TITLE_MISSING)), result)
        assertTrue(photoStore.deletedPhotos.isEmpty())
    }

    @Test
    fun `an unknown incident is reported as not found`() = runTest {
        assertEquals(SaveIncidentResult.NotFound, updateIncident("unknown", testDraft()))
    }
}
