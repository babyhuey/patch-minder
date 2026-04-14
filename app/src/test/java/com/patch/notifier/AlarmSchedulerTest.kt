package com.patch.notifier

import com.patch.notifier.alarm.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

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
    fun `patch and nag request codes never collide for valid patch ids`() {
        for (id in 1..4) {
            assertNotEquals(
                "Patch and nag codes should not collide for id=$id",
                AlarmScheduler.patchAlarmRequestCode(id),
                AlarmScheduler.nagAlarmRequestCode(id),
            )
        }
    }

    @Test
    fun `patch alarm codes are unique across all patch ids`() {
        val codes = (1..4).map { AlarmScheduler.patchAlarmRequestCode(it) }.toSet()
        assertEquals(4, codes.size)
    }

    @Test
    fun `nag alarm codes are unique across all patch ids`() {
        val codes = (1..4).map { AlarmScheduler.nagAlarmRequestCode(it) }.toSet()
        assertEquals(4, codes.size)
    }

    @Test
    fun `no overlap between any patch code and any nag code`() {
        val patchCodes = (1..4).map { AlarmScheduler.patchAlarmRequestCode(it) }.toSet()
        val nagCodes = (1..4).map { AlarmScheduler.nagAlarmRequestCode(it) }.toSet()
        val overlap = patchCodes.intersect(nagCodes)
        assertEquals("Should have no overlapping codes", emptySet<Int>(), overlap)
    }

    @Test
    fun `request code for patch id 0`() {
        assertEquals(1000, AlarmScheduler.patchAlarmRequestCode(0))
        assertEquals(2000, AlarmScheduler.nagAlarmRequestCode(0))
    }

    @Test
    fun `nag delay is 1 hour for first nag then 2 hours`() {
        assertEquals(3600000L, AlarmScheduler.nagDelayMs(nagCount = 0))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 1))
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 5))
    }

    @Test
    fun `nag delay with negative count returns repeat delay`() {
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = -1))
    }

    @Test
    fun `nag delay for large count still returns repeat delay`() {
        assertEquals(7200000L, AlarmScheduler.nagDelayMs(nagCount = 100))
    }

    @Test
    fun `first nag delay is exactly 1 hour in ms`() {
        assertEquals(60 * 60 * 1000L, AlarmScheduler.nagDelayMs(0))
    }

    @Test
    fun `repeat nag delay is exactly 2 hours in ms`() {
        assertEquals(2 * 60 * 60 * 1000L, AlarmScheduler.nagDelayMs(1))
    }

    // --- adjustToNotifyTime ---

    @Test
    fun `adjustToNotifyTime sets correct hour and minute`() {
        val dueAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val adjusted = AlarmScheduler.adjustToNotifyTime(dueAt, 9, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }

    @Test
    fun `adjustToNotifyTime result is in the future`() {
        val dueAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val adjusted = AlarmScheduler.adjustToNotifyTime(dueAt, 9, 0)
        assertTrue("Adjusted time should be in the future", adjusted > System.currentTimeMillis())
    }

    @Test
    fun `adjustToNotifyTime midnight wraps correctly`() {
        val dueAt = System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000
        val adjusted = AlarmScheduler.adjustToNotifyTime(dueAt, 0, 0)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
    }
}
