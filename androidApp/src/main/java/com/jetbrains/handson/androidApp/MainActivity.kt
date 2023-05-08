package com.jetbrains.handson.androidApp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.jetbrains.handson.kmm.shared.SpaceXSDK
import com.jetbrains.handson.kmm.shared.cache.DatabaseDriverFactory
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import androidx.core.view.isVisible
import com.jetbrains.handson.kmm.shared.entity.RocketLaunch
import kotlinx.coroutines.cancel


class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private val mainScope = MainScope()

    private lateinit var launchesRecyclerView: RecyclerView
    private lateinit var progressBarView: FrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout

    private val sdk = SpaceXSDK(DatabaseDriverFactory(this))

    private val launchesRvAdapter = LaunchesRvAdapter(this, listOf())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "SpaceX Launches"
        setContentView(R.layout.activity_main)

        launchesRecyclerView = findViewById(R.id.launchesListRv)
        progressBarView = findViewById(R.id.progressBar)
        swipeRefreshLayout = findViewById(R.id.swipeContainer)

        launchesRvAdapter.callback = object : LaunchesRvAdapter.Callback {
            override fun onClickCloseIcon(launch: RocketLaunch) {
                Toast.makeText(this@MainActivity, "Clicked on close icon", Toast.LENGTH_SHORT).show()
                mainScope.launch {
                    sdk.removeLaunch(launch)
//                    getData(GetDataType.LOAD_FROM_DB)
                }
            }
        }

        launchesRecyclerView.adapter = launchesRvAdapter
        launchesRecyclerView.layoutManager = LinearLayoutManager(this)

        observerData()

        swipeRefreshLayout.setOnRefreshListener {
            swipeRefreshLayout.isRefreshing = false
            getData(GetDataType.FETCH_FROM_INTERNET)
        }

        getData(GetDataType.FETCH_FROM_INTERNET)

    }

    enum class GetDataType {
        LOAD_FROM_DB, FETCH_FROM_INTERNET
    }

    override fun onDestroy() {
        super.onDestroy()
        mainScope.cancel()
    }

    private fun observerData() {
        mainScope.launch {
            sdk.observeAllLaunchesFromDb().collect {
                Log.d(TAG, "COLLECT: n = ${it.size}")
                launchesRvAdapter.launches = it
                launchesRvAdapter.notifyDataSetChanged()
                updateActivityTitle(it.size)
            }
        }
    }

    private fun getData(getType: GetDataType) {
        progressBarView.isVisible = true
        mainScope.launch {
            kotlin.runCatching {
                if (getType == GetDataType.LOAD_FROM_DB) {
                    sdk.loadLaunchesFromDb()
                } else {
                    sdk.fetchLaunchesFromInternet()
                }
            }.onSuccess {
                val numberOfLaunches = it.size
//                updateActivityTitle(numberOfLaunches)
//                launchesRvAdapter.launches = it
//                launchesRvAdapter.notifyDataSetChanged()
            }.onFailure {
                Toast.makeText(this@MainActivity, it.localizedMessage, Toast.LENGTH_SHORT).show()
            }
            progressBarView.isVisible = false
        }
    }

    private fun updateActivityTitle(numberOfLaunches: Int) {
        title = "SpaceX Launches ($numberOfLaunches)"
    }
}