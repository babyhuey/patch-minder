package com.patch.notifier

import androidx.core.app.NotificationCompat
import com.patch.notifier.alarm.AlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmReceiverTest {

    private val now = 1_000_000_000_000L

    // --- shouldSuppressNag ---

    @Test
    fun `suppress - null dueAt does not suppress`() {
        assertFalse(AlarmReceiver.shouldSuppressNag(null, now))
    }

    @Test
    fun `suppress - dueAt well in the future suppresses`() {
        val dueAt = now + 7L * 24 * 60 * 60 * 1000 // 7 days ahead
        assertTrue(AlarmReceiver.shouldSuppressNag(dueAt, now))
    }

    @Test
    fun `suppress - dueAt in the past does not suppress`() {
        val dueAt = now - 3600_000 // 1 hour ago
        assertFalse(AlarmReceiver.shouldSuppressNag(dueAt, now))
    }

    @Test
    fun `suppress - dueAt exactly at now does not suppress`() {
        assertFalse(AlarmReceiver.shouldSuppressNag(now, now))
    }

    @Test
    fun `suppress - dueAt 59 seconds ahead does not suppress`() {
        assertFalse(AlarmReceiver.shouldSuppressNag(now + 59_000, now))
    }

    @Test
    fun `suppress - dueAt 60 seconds ahead does not suppress (boundary)`() {
        assertFalse(AlarmReceiver.shouldSuppressNag(now + 60_000, now))
    }

    @Test
    fun `suppress - dueAt 61 seconds ahead suppresses`() {
        assertTrue(AlarmReceiver.shouldSuppressNag(now + 61_000, now))
    }

    @Test
    fun `suppress - dueAt 1ms ahead does not suppress`() {
        assertFalse(AlarmReceiver.shouldSuppressNag(now + 1, now))
    }

    // --- shouldScheduleNextNag ---

    @Test
    fun `nag schedule - count 0 schedules`() {
        assertTrue(AlarmReceiver.shouldScheduleNextNag(0))
    }

    @Test
    fun `nag schedule - count 47 schedules`() {
        assertTrue(AlarmReceiver.shouldScheduleNextNag(47))
    }

    @Test
    fun `nag schedule - count 48 does not schedule (at limit)`() {
        assertFalse(AlarmReceiver.shouldScheduleNextNag(48))
    }

    @Test
    fun `nag schedule - count 100 does not schedule`() {
        assertFalse(AlarmReceiver.shouldScheduleNextNag(100))
    }

    @Test
    fun `nag schedule - negative count schedules`() {
        assertTrue(AlarmReceiver.shouldScheduleNextNag(-1))
    }

    @Test
    fun `nag limit constant is 48`() {
        assertEquals(48, AlarmReceiver.NAG_LIMIT)
    }

    // --- notificationTitle ---

    @Test
    fun `title - primary notification`() {
        assertEquals("Patch Replacement Due", AlarmReceiver.notificationTitle(false))
    }

    @Test
    fun `title - nag notification`() {
        assertEquals("Patch Still Needs Replacing!", AlarmReceiver.notificationTitle(true))
    }

    // --- notificationBody ---

    @Test
    fun `body - includes location name`() {
        assertEquals("Time to replace: Left Upper", AlarmReceiver.notificationBody("Left Upper"))
    }

    @Test
    fun `body - different location`() {
        assertEquals("Time to replace: Right Lower", AlarmReceiver.notificationBody("Right Lower"))
    }

    // --- notificationPriority ---

    @Test
    fun `priority - first notification is HIGH`() {
        assertEquals(NotificationCompat.PRIORITY_HIGH, AlarmReceiver.notificationPriority(0))
    }

    @Test
    fun `priority - second nag is still HIGH`() {
        assertEquals(NotificationCompat.PRIORITY_HIGH, AlarmReceiver.notificationPriority(1))
    }

    @Test
    fun `priority - third nag escalates to MAX`() {
        assertEquals(NotificationCompat.PRIORITY_MAX, AlarmReceiver.notificationPriority(2))
    }

    @Test
    fun `priority - later nags stay MAX`() {
        assertEquals(NotificationCompat.PRIORITY_MAX, AlarmReceiver.notificationPriority(10))
    }
}
