package com.patch.notifier

import com.patch.notifier.alarm.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmSchedulerTest {

    @Test
    fun `request code for patch alarm uses patch id`() {
        assertEquals(1001, AlarmScheduler.patchAlarmRequestCode(1))
        assertEquals(1004, AlarmScheduler.patchAlarmRequestCode(4))
    }

    @Test
    fun `request code for nag alarm uses offset`() {
        assertEquals(2001, AlarmScheduler.nagAlarmRequestCode(1))
        assertEquals(2004, AlarmScheduler.nagAlarmRequestCode(4))
    }

    @Test
    fun `nag delay is 1 hour for first nag then 2 hours`() {
        assertEquals(3600000L, AlarmScheduler.nagDelayMs(nagCount = 0))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 1))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 5))
    }
}
