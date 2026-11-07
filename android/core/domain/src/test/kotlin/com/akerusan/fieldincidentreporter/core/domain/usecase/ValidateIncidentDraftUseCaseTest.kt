package com.akerusan.fieldincidentreporter.core.domain.usecase

import com.akerusan.fieldincidentreporter.core.domain.result.DraftError
import com.akerusan.fieldincidentreporter.core.model.IncidentDraft
import com.akerusan.fieldincidentreporter.core.model.IncidentPhoto
import com.akerusan.fieldincidentreporter.core.model.IncidentStatus
import com.akerusan.fieldincidentreporter.core.testing.testDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidateIncidentDraftUseCaseTest {
    private val validate = ValidateIncidentDraftUseCase()

    @Test
    fun `a complete draft is valid`() {
        assertTrue(validate(testDraft()).isEmpty())
    }

    @Test
    fun `an empty draft reports every missing field`() {
        assertEquals(
            setOf(DraftError.TITLE_MISSING, DraftError.CATEGORY_MISSING, DraftError.SEVERITY_MISSING),
            validate(IncidentDraft()),
        )
    }

    @Test
    fun `a blank title counts as missing`() {
        assertEquals(setOf(DraftError.TITLE_MISSING), validate(testDraft(title = "   ")))
    }

    @Test
    fun `length limits apply`() {
        val draft = testDraft(title = "x".repeat(ValidateIncidentDraftUseCase.MAX_TITLE_LENGTH + 1)).copy(
            description = "x".repeat(ValidateIncidentDraftUseCase.MAX_DESCRIPTION_LENGTH + 1),
            locationNote = "x".repeat(ValidateIncidentDraftUseCase.MAX_LOCATION_LENGTH + 1),
        )
        assertEquals(
            setOf(DraftError.TITLE_TOO_LONG, DraftError.DESCRIPTION_TOO_LONG, DraftError.LOCATION_TOO_LONG),
            validate(draft),
        )
    }

    @Test
    fun `technicians can't set RESOLVED`() {
        assertEquals(setOf(DraftError.STATUS_NOT_ALLOWED), validate(testDraft(status = IncidentStatus.RESOLVED)))
    }

    @Test
    fun `the number of photos is limited`() {
        val photos = List(ValidateIncidentDraftUseCase.MAX_PHOTOS + 1) { IncidentPhoto(id = "p$it") }
        assertEquals(setOf(DraftError.TOO_MANY_PHOTOS), validate(testDraft(photos = photos)))
    }
}
