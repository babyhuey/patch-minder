package com.patch.notifier

import com.patch.notifier.ui.formatDaysLeft
import com.patch.notifier.ui.formatStatusInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayUtilsTest {

    private val now = 1_000_000_000_000L // fixed timestamp

    // --- formatStatusInfo ---

    @Test
    fun `status - 7 days from now`() {
        val dueAt = now + 7L * 24 * 60 * 60 * 1000
        val status = formatStatusInfo(dueAt, 3, now)
        assertEquals("NEXT REPLACEMENT", status.headerLabel)
        assertEquals("3 patches · 7d from now", status.timeText)
        assertFalse(status.isOverdue)
    }

    @Test
    fun `status - 1 day from now shows tomorrow`() {
        val dueAt = now + 1L * 24 * 60 * 60 * 1000 + 1000 // just over 1 day
        val status = formatStatusInfo(dueAt, 2, now)
        assertEquals("2 patches · tomorrow", status.timeText)
        assertFalse(status.isOverdue)
    }

    @Test
    fun `status - due today (less than 24h away)`() {
        val dueAt = now + 12L * 60 * 60 * 1000 // 12 hours from now
        val status = formatStatusInfo(dueAt, 1, now)
        assertEquals("1 patch · due today", status.timeText)
        assertFalse(status.isOverdue)
    }

    @Test
    fun `status - due in 1 minute still shows due today`() {
        val dueAt = now + 60_000
        val status = formatStatusInfo(dueAt, 3, now)
        assertEquals("3 patches · due today", status.timeText)
        assertFalse(status.isOverdue)
    }

    @Test
    fun `status - overdue by 1 millisecond`() {
        val dueAt = now - 1
        val status = formatStatusInfo(dueAt, 2, now)
        assertEquals("OVERDUE", status.headerLabel)
        assertEquals("2 patches · overdue!", status.timeText)
        assertTrue(status.isOverdue)
    }

    @Test
    fun `status - overdue by 12 hours`() {
        val dueAt = now - 12L * 60 * 60 * 1000
        val status = formatStatusInfo(dueAt, 3, now)
        assertTrue(status.isOverdue)
        assertEquals("3 patches · overdue!", status.timeText)
    }

    @Test
    fun `status - overdue by 3 days`() {
        val dueAt = now - 3L * 24 * 60 * 60 * 1000
        val status = formatStatusInfo(dueAt, 1, now)
        assertTrue(status.isOverdue)
    }

    @Test
    fun `status - exactly now`() {
        val status = formatStatusInfo(now, 1, now)
        assertEquals("1 patch · due today", status.timeText)
        assertFalse(status.isOverdue)
    }

    // --- formatDaysLeft ---

    @Test
    fun `daysLeft - 7 days from now`() {
        val dueAt = now + 7L * 24 * 60 * 60 * 1000
        assertEquals("7d left", formatDaysLeft(dueAt, now))
    }

    @Test
    fun `daysLeft - 1 day from now`() {
        val dueAt = now + 1L * 24 * 60 * 60 * 1000 + 1000
        assertEquals("1 day left", formatDaysLeft(dueAt, now))
    }

    @Test
    fun `daysLeft - due today (less than 24h)`() {
        val dueAt = now + 5L * 60 * 60 * 1000
        assertEquals("due today", formatDaysLeft(dueAt, now))
    }

    @Test
    fun `daysLeft - overdue by 1ms`() {
        assertEquals("overdue!", formatDaysLeft(now - 1, now))
    }

    @Test
    fun `daysLeft - overdue by 23 hours`() {
        val dueAt = now - 23L * 60 * 60 * 1000
        assertEquals("overdue!", formatDaysLeft(dueAt, now))
    }

    @Test
    fun `daysLeft - exactly now`() {
        assertEquals("due today", formatDaysLeft(now, now))
    }
}
