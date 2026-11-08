package com.akerusan.fieldincidentreporter.core.domain

/** Generates unique ids on the device, so incidents and photos can be created offline. */
fun interface IdGenerator {
    fun newId(): String
}
