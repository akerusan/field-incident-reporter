package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.testing.FakePhotoStore
import com.akerusan.fieldincidentreporter.core.testing.FixedIdGenerator
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhotoUseCasesTest {
    private val photoStore = FakePhotoStore()

    @Test
    fun `an added photo gets a device-generated id`() = runTest {
        val photo = AddPhotoUseCase(photoStore, FixedIdGenerator("photo"))("content://picker/1")

        assertEquals(IncidentPhoto(id = "photo-1", localPath = "/photos/photo-1.jpg"), photo)
    }

    @Test
    fun `an unreadable image adds nothing`() = runTest {
        photoStore.unreadableUris += "content://picker/broken"

        assertNull(AddPhotoUseCase(photoStore, FixedIdGenerator())("content://picker/broken"))
    }

    @Test
    fun `discarding a draft deletes only the photos added in this session`() = runTest {
        val saved = IncidentPhoto(id = "saved", localPath = "/photos/saved.jpg")
        val added = IncidentPhoto(id = "added", localPath = "/photos/added.jpg")

        DiscardDraftUseCase(photoStore)(testDraft(photos = listOf(saved, added)), savedPhotoIds = setOf("saved"))

        assertEquals(listOf(added), photoStore.deletedPhotos)
    }
}
