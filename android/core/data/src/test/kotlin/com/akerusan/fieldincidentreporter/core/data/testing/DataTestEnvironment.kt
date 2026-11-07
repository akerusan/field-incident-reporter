package com.akerusan.fieldincidentreporter.core.data.testing

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.akerusan.fieldincidentreporter.core.data.mapper.IncidentMapper
import com.akerusan.fieldincidentreporter.core.data.repository.OfflineFirstIncidentRepository
import com.akerusan.fieldincidentreporter.core.data.sync.IncidentSynchronizer
import com.akerusan.fieldincidentreporter.core.database.DatabaseTransactionRunner
import com.akerusan.fieldincidentreporter.core.database.FirDatabase
import com.akerusan.fieldincidentreporter.core.network.ApiErrorParser
import com.akerusan.fieldincidentreporter.core.network.IncidentApi
import com.akerusan.fieldincidentreporter.core.network.NetworkFactory
import com.akerusan.fieldincidentreporter.core.network.RemotePathResolver
import com.akerusan.fieldincidentreporter.core.testing.FakeClock
import com.akerusan.fieldincidentreporter.core.testing.FakePhotoStore
import com.akerusan.fieldincidentreporter.core.testing.FakeSyncScheduler
import java.io.Closeable
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient

/**
 * The real repository and synchronizer wired to an in-memory database and a scripted mock server.
 * Register responses with [respond]; requests without a handler get a 404.
 */
class DataTestEnvironment : Closeable {
    val database: FirDatabase = Room
        .inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), FirDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    val dao = database.incidentDao()
    val photoStore = FakePhotoStore()
    val clock = FakeClock()
    val syncScheduler = FakeSyncScheduler()

    private val handlers = mutableMapOf<String, (RecordedRequest) -> MockResponse>()
    val requests = CopyOnWriteArrayList<String>()

    val server = MockWebServer().apply {
        dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val key = "${request.method} ${request.url.encodedPath}"
                requests += key
                return handlers[key]?.invoke(request) ?: MockResponse.Builder().code(404).body("{}").build()
            }
        }
        start()
    }

    private val json = NetworkFactory.json() // the production settings
    private val api: IncidentApi = NetworkFactory.incidentApi(server.url("/").toString(), OkHttpClient(), json)

    val mapper = IncidentMapper(RemotePathResolver(server.url("/").toString()))
    private val transaction = DatabaseTransactionRunner(database)

    val repository = OfflineFirstIncidentRepository(
        dao,
        transaction,
        api,
        mapper,
        photoStore,
        syncScheduler,
        ioDispatcher = Dispatchers.IO,
    )
    val synchronizer = IncidentSynchronizer(dao, transaction, api, mapper, ApiErrorParser(json), photoStore, clock)

    /** Answers `METHOD /path` with [handler]. The handler runs on the server's thread. */
    fun respond(method: String, path: String, handler: (RecordedRequest) -> MockResponse) {
        handlers["$method $path"] = handler
    }

    fun respond(method: String, path: String, code: Int, body: String = "{}") =
        respond(method, path) { MockResponse.Builder().code(code).body(body).build() }

    /** Simulates losing the connection: every request now fails with an IOException. */
    fun goOffline() = server.close()

    override fun close() {
        database.close()
        server.close()
    }
}
