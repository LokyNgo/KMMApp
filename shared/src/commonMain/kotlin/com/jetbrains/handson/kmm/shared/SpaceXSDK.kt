package com.jetbrains.handson.kmm.shared

import com.jetbrains.handson.kmm.shared.cache.CommonFlow
import com.jetbrains.handson.kmm.shared.cache.Database
import com.jetbrains.handson.kmm.shared.cache.DatabaseDriverFactory
import com.jetbrains.handson.kmm.shared.entity.RocketLaunch
import com.jetbrains.handson.kmm.shared.network.SpaceXApi
import kotlinx.coroutines.*


class SpaceXSDK (databaseDriverFactory: DatabaseDriverFactory) {
    private val database = Database(databaseDriverFactory)
    private val api = SpaceXApi()

    @Throws(Exception::class) suspend fun getLaunches(forceReload: Boolean): List<RocketLaunch> {
        val cachedLaunches = database.getAllLaunches()
        return if (cachedLaunches.isNotEmpty() && !forceReload) {
            cachedLaunches
        } else {
            api.getAllLaunches().also {
                database.clearDatabase()
                database.createLaunches(it)
            }
        }
    }

    @Throws(Exception::class) suspend fun loadLaunchesFromDb(): List<RocketLaunch> {
        return database.getAllLaunches()
    }

    @Throws(Exception::class) fun observeAllLaunchesFromDb(): CommonFlow<List<RocketLaunch>> {
        return database.getAllLaunchesFlow()
    }

    @Throws(Exception::class) suspend fun fetchLaunchesFromInternet(): List<RocketLaunch> {
        return api.getAllLaunches().also {
            database.clearDatabaseThenInsertLaunches(it)
        }
    }

    @Throws(Exception::class) fun removeLaunch(launch: RocketLaunch) {
        database.deleteLaunch(launch)
    }

}


