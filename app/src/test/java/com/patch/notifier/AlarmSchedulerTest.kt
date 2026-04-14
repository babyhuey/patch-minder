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
    // Uses a fixed dueAt: 2025-06-15 14:30:00 UTC

    private fun fixedDueAt(): Long {
        return Calendar.getInstance().apply {
            set(2025, Calendar.JUNE, 15, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `adjustToNotifyTime sets correct hour and minute`() {
        val adjusted = AlarmScheduler.adjustToNotifyTime(fixedDueAt(), 9, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }

    @Test
    fun `adjustToNotifyTime - preferred time before dueAt on same day pushes to next day`() {
        // dueAt is 14:30, preferred is 9:00 -> 9:00 on Jun 15 is before 14:30 -> push to Jun 16
        val adjusted = AlarmScheduler.adjustToNotifyTime(fixedDueAt(), 9, 0)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH)) // next day
        assertEquals(9, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `adjustToNotifyTime - preferred time after dueAt on same day stays on same day`() {
        // dueAt is 14:30, preferred is 18:00 -> 18:00 on Jun 15 is after 14:30 -> same day
        val adjusted = AlarmScheduler.adjustToNotifyTime(fixedDueAt(), 18, 0)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH)) // same day
        assertEquals(18, cal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `adjustToNotifyTime - result is always at or after dueAt`() {
        val dueAt = fixedDueAt()
        for (hour in 0..23) {
            val adjusted = AlarmScheduler.adjustToNotifyTime(dueAt, hour, 0)
            assertTrue(
                "Adjusted ($adjusted) should be >= dueAt ($dueAt) for hour=$hour",
                adjusted >= dueAt,
            )
        }
    }

    @Test
    fun `adjustToNotifyTime midnight wraps correctly`() {
        // dueAt is 14:30 on Jun 15, preferred 0:00 -> 0:00 on Jun 15 is before 14:30 -> push to Jun 16 0:00
        val adjusted = AlarmScheduler.adjustToNotifyTime(fixedDueAt(), 0, 0)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(0, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(Calendar.MINUTE))
        assertEquals(16, cal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `adjustToNotifyTime - exact same time as dueAt stays on same day`() {
        // dueAt is 14:30, preferred is 14:30 -> equal, not less than -> same day
        val adjusted = AlarmScheduler.adjustToNotifyTime(fixedDueAt(), 14, 30)
        val cal = Calendar.getInstance().apply { timeInMillis = adjusted }
        assertEquals(15, cal.get(Calendar.DAY_OF_MONTH))
        assertEquals(14, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, cal.get(Calendar.MINUTE))
    }
}
