package com.akerusan.fieldincidentreporter.core.data.util

import com.akerusan.fieldincidentreporter.core.domain.IdGenerator
import java.util.UUID
import javax.inject.Inject

/** Random (version 4) UUIDs: unique without coordinating with the server. */
class UuidIdGenerator @Inject constructor() : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
