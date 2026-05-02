package com.fogalarm

import org.junit.Assert.*
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun `calculateAlarmTime subtracts lead time in millis`() {
        val fogStart = 1_700_000_000_000L
        val result = AlarmScheduler.calculateAlarmTime(fogStart, 60)
        assertEquals(fogStart - 60 * 60_000L, result)
    }

    @Test
    fun `30 minute lead time`() {
        val fogStart = 1_700_000_000_000L
        assertEquals(fogStart - 30 * 60_000L, AlarmScheduler.calculateAlarmTime(fogStart, 30))
    }

    @Test
    fun `120 minute lead time`() {
        val fogStart = 1_700_000_000_000L
        assertEquals(fogStart - 120 * 60_000L, AlarmScheduler.calculateAlarmTime(fogStart, 120))
    }

    @Test
    fun `zero lead time returns fog start time`() {
        val fogStart = 1_700_000_000_000L
        assertEquals(fogStart, AlarmScheduler.calculateAlarmTime(fogStart, 0))
    }
}
