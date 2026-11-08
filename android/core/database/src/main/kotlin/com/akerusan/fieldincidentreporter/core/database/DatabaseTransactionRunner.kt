package com.akerusan.fieldincidentreporter.core.database

import androidx.room.withTransaction
import javax.inject.Inject

/** Runs a block of DAO calls atomically, without exposing Room to the modules that call it. */
class DatabaseTransactionRunner @Inject constructor(private val database: FirDatabase) {
    suspend operator fun <T> invoke(block: suspend () -> T): T = database.withTransaction(block)
}
