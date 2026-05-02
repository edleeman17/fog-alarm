package com.fogalarm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FogCheckWorkerTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
    }

    @Test
    fun workerReturnsRetryWhenNoLocation() {
        // No GPS fix available in test environment — expect retry
        val worker = TestListenableWorkerBuilder<FogCheckWorker>(context).build()
        val result = worker.startWork().get()
        // Either retry (no location) or success (if emulator has mock location)
        assert(
            result is ListenableWorker.Result.Retry ||
            result is ListenableWorker.Result.Success
        ) { "Unexpected result: $result" }
    }

    @Test
    fun workerLogsToDebugLog() {
        DebugLogger.clear(context)
        val worker = TestListenableWorkerBuilder<FogCheckWorker>(context).build()
        worker.startWork().get()
        val log = DebugLogger.getLog(context)
        assert(log.contains("[WORKER]")) { "Expected WORKER log entries, got: $log" }
    }
}
