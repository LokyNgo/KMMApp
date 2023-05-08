package com.jetbrains.handson.kmm.shared.cache

import com.jetbrains.handson.kmm.shared.entity.Links
import com.jetbrains.handson.kmm.shared.entity.Patch
import com.jetbrains.handson.kmm.shared.entity.RocketLaunch
import com.squareup.sqldelight.db.Closeable
import com.squareup.sqldelight.runtime.coroutines.asFlow
import com.squareup.sqldelight.runtime.coroutines.mapToList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

internal class Database(databaseDriverFactory: DatabaseDriverFactory) {
    private val database = AppDatabase(databaseDriverFactory.createDriver())
    private val dbQuery = database.appDatabaseQueries

    internal fun clearDatabase() {
        dbQuery.transaction {
            dbQuery.removeAllLaunches()
        }
    }

    internal fun getAllLaunches(): List<RocketLaunch> {
        return dbQuery.selectAllLaunchesInfo(::mapLaunchSelecting).executeAsList()
    }

    internal fun getAllLaunchesFlow(): CommonFlow<List<RocketLaunch>> {
        return dbQuery.selectAllLaunchesInfo(::mapLaunchSelecting)
            .asFlow()
            .mapToList()
            .asCommonFlow()
    }

    private fun mapLaunchSelecting(
        flightNumber: Long,
        missionName: String,
        details: String?,
        launchSuccess: Boolean?,
        launchDateUTC: String,
        patchUrlSmall: String?,
        patchUrlLarge: String?,
        articleUrl: String?
    ): RocketLaunch {
        return RocketLaunch(
            flightNumber = flightNumber.toInt(),
            missionName = missionName,
            details = details,
            launchDateUTC = launchDateUTC,
            launchSuccess = launchSuccess,
            links = Links(
                patch = Patch(
                    small = patchUrlSmall,
                    large = patchUrlLarge
                ),
                article = articleUrl
            )
        )
    }

    internal fun createLaunches(launches: List<RocketLaunch>) {
        dbQuery.transaction {
            launches.forEach { launch ->
                insertLaunch(launch)
            }
        }
    }

    private fun insertLaunch(launch: RocketLaunch) {
        dbQuery.insertLaunch(
            flightNumber = launch.flightNumber.toLong(),
            missionName = launch.missionName,
            details = launch.details,
            launchSuccess = launch.launchSuccess ?: false,
            launchDateUTC = launch.launchDateUTC,
            patchUrlSmall = launch.links.patch?.small,
            patchUrlLarge = launch.links.patch?.large,
            articleUrl = launch.links.article
        )
    }

    internal fun clearDatabaseThenInsertLaunches(launches: List<RocketLaunch>) {
        dbQuery.transaction {
            dbQuery.removeAllLaunches()
            launches.forEach { launch ->
                insertLaunch(launch)
            }
        }
    }

    internal fun deleteLaunch(launch: RocketLaunch) {
        dbQuery.removeLaunchFromFlightNumber(launch.flightNumber.toLong())
    }
}

fun <T> Flow<T>.asCommonFlow(): CommonFlow<T> = CommonFlow(this)
class CommonFlow<T>(private val origin: Flow<T>) : Flow<T> by origin {
    fun watch(block: (T) -> Unit): Closeable {
        val job = Job()

        onEach {
            block(it)
        }.launchIn(CoroutineScope(Dispatchers.Main + job))

        return object : Closeable {
            override fun close() {
                job.cancel()
            }
        }
    }
}