package com.patch.notifier

import com.patch.notifier.widget.PatchWidgetState
import com.patch.notifier.widget.WidgetUrgency
import com.patch.notifier.widget.computeWidgetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatchWidgetStateTest {

    private val now = 1_000_000_000_000L

    // --- computeWidgetState ---

    @Test
    fun `no active patches - null dueAt`() {
        val state = computeWidgetState(null, 0, now)
        assertNull(state.daysUntilDue)
        assertEquals(0, state.activePatchCount)
    }

    @Test
    fun `7 days out`() {
        val dueAt = now + 7L * 24 * 60 * 60 * 1000
        val state = computeWidgetState(dueAt, 3, now)
        assertEquals(7L, state.daysUntilDue)
        assertEquals(3, state.activePatchCount)
    }

    @Test
    fun `1 day out`() {
        val dueAt = now + 1L * 24 * 60 * 60 * 1000 + 1000
        val state = computeWidgetState(dueAt, 2, now)
        assertEquals(1L, state.daysUntilDue)
    }

    @Test
    fun `due today - less than 24h`() {
        val dueAt = now + 12L * 60 * 60 * 1000
        val state = computeWidgetState(dueAt, 3, now)
        assertEquals(0L, state.daysUntilDue)
    }

    @Test
    fun `overdue - returns -1`() {
        val dueAt = now - 3L * 60 * 60 * 1000
        val state = computeWidgetState(dueAt, 3, now)
        assertEquals(-1L, state.daysUntilDue)
    }

    @Test
    fun `overdue by days - still returns -1`() {
        val dueAt = now - 3L * 24 * 60 * 60 * 1000
        val state = computeWidgetState(dueAt, 1, now)
        assertEquals(-1L, state.daysUntilDue)
    }

    @Test
    fun `exactly now - returns 0`() {
        val state = computeWidgetState(now, 2, now)
        assertEquals(0L, state.daysUntilDue)
    }

    // --- urgency ---

    @Test
    fun `urgency NONE when no patches`() {
        val state = PatchWidgetState(null, 0)
        assertEquals(WidgetUrgency.NONE, state.urgency)
    }

    @Test
    fun `urgency GOOD when 7 days out`() {
        val state = PatchWidgetState(7L, 3)
        assertEquals(WidgetUrgency.GOOD, state.urgency)
    }

    @Test
    fun `urgency GOOD when 2 days out`() {
        val state = PatchWidgetState(2L, 3)
        assertEquals(WidgetUrgency.GOOD, state.urgency)
    }

    @Test
    fun `urgency SOON when 1 day out`() {
        val state = PatchWidgetState(1L, 3)
        assertEquals(WidgetUrgency.SOON, state.urgency)
    }

    @Test
    fun `urgency SOON when due today`() {
        val state = PatchWidgetState(0L, 3)
        assertEquals(WidgetUrgency.SOON, state.urgency)
    }

    @Test
    fun `urgency OVERDUE when negative`() {
        val state = PatchWidgetState(-1L, 3)
        assertEquals(WidgetUrgency.OVERDUE, state.urgency)
    }

    // --- displayText ---

    @Test
    fun `displayText no patches`() {
        assertEquals("—", PatchWidgetState(null, 0).displayText)
    }

    @Test
    fun `displayText 7 days`() {
        assertEquals("7d", PatchWidgetState(7L, 3).displayText)
    }

    @Test
    fun `displayText 0 days`() {
        assertEquals("0d", PatchWidgetState(0L, 3).displayText)
    }

    @Test
    fun `displayText overdue`() {
        assertEquals("!", PatchWidgetState(-1L, 2).displayText)
    }

    // --- subtitleText ---

    @Test
    fun `subtitleText no patches`() {
        assertEquals("no patches", PatchWidgetState(null, 0).subtitleText)
    }

    @Test
    fun `subtitleText 7 days`() {
        assertEquals("left", PatchWidgetState(7L, 3).subtitleText)
    }

    @Test
    fun `subtitleText 1 day`() {
        assertEquals("tomorrow", PatchWidgetState(1L, 3).subtitleText)
    }

    @Test
    fun `subtitleText today`() {
        assertEquals("today", PatchWidgetState(0L, 3).subtitleText)
    }

    @Test
    fun `subtitleText overdue`() {
        assertEquals("overdue", PatchWidgetState(-1L, 2).subtitleText)
    }
}
