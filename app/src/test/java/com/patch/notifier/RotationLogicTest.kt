package com.patch.notifier

import com.patch.notifier.data.PATCH_DURATION_MS
import com.patch.notifier.data.Patch
import com.patch.notifier.data.suggestLocations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RotationLogicTest {

    @Test
    fun `first launch - all null appliedAt - suggests first 3 by id`() {
        val patches = listOf(
            Patch(1, "Left Upper", null, null),
            Patch(2, "Left Lower", null, null),
            Patch(3, "Right Upper", null, null),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches)
        assertEquals(3, suggested.size)
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `after first use - rests the most recently applied`() {
        val now = System.currentTimeMillis()
        val patches = listOf(
            Patch(1, "Left Upper", now, now + 604800000),
            Patch(2, "Left Lower", now, now + 604800000),
            Patch(3, "Right Upper", now, now + 604800000),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches)
        assertEquals(3, suggested.size)
        assertTrue("Should suggest the unused location", 4 in suggested)
    }

    @Test
    fun `rotation cycle - oldest applied gets suggested`() {
        val week1 = 1000L
        val week2 = 604800000L + 1000L
        val patches = listOf(
            Patch(1, "Left Upper", week1, week1 + 604800000),
            Patch(2, "Left Lower", week1, week1 + 604800000),
            Patch(3, "Right Upper", week1, week1 + 604800000),
            Patch(4, "Right Lower", week2, week2 + 604800000),
        )
        val suggested = suggestLocations(patches)
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `selecting fewer than 3 - works with count 1`() {
        val patches = listOf(
            Patch(1, "Left Upper", null, null),
            Patch(2, "Left Lower", null, null),
            Patch(3, "Right Upper", null, null),
            Patch(4, "Right Lower", null, null),
        )
        val suggested = suggestLocations(patches, count = 1)
        assertEquals(1, suggested.size)
    }

    @Test
    fun `all patches overdue - suggests oldest first`() {
        val oldTime = 1000L
        val patches = listOf(
            Patch(1, "Left Upper", oldTime, oldTime + 604800000),
            Patch(2, "Left Lower", oldTime + 100, oldTime + 604800100),
            Patch(3, "Right Upper", oldTime + 200, oldTime + 604800200),
            Patch(4, "Right Lower", oldTime + 300, oldTime + 604800300),
        )
        val suggested = suggestLocations(patches)
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `count larger than list size - returns all patches`() {
        val patches = listOf(
            Patch(1, "Left Upper", null, null),
            Patch(2, "Left Lower", null, null),
        )
        val suggested = suggestLocations(patches, count = 5)
        assertEquals(2, suggested.size)
        assertEquals(listOf(1, 2), suggested)
    }

    @Test
    fun `stable sort - same appliedAt orders by id`() {
        val now = 1000L
        val patches = listOf(
            Patch(3, "Right Upper", now, now + 604800000),
            Patch(1, "Left Upper", now, now + 604800000),
            Patch(4, "Right Lower", now, now + 604800000),
            Patch(2, "Left Lower", now, now + 604800000),
        )
        val suggested = suggestLocations(patches)
        assertEquals(listOf(1, 2, 3), suggested)
    }

    @Test
    fun `empty list returns empty`() {
        assertEquals(emptyList<Int>(), suggestLocations(emptyList()))
    }

    @Test
    fun `count zero returns empty`() {
        val patches = listOf(Patch(1, "Left Upper", null, null))
        assertEquals(emptyList<Int>(), suggestLocations(patches, count = 0))
    }

    @Test
    fun `negative count returns empty`() {
        val patches = listOf(Patch(1, "Left Upper", null, null))
        assertEquals(emptyList<Int>(), suggestLocations(patches, count = -1))
    }

    @Test
    fun `removing one patch leaves others' due dates intact and re-suggests removed slot`() {
        val now = 1_000_000_000L
        val due = now + 604_800_000L
        val patches = listOf(
            Patch(1, "Left Upper", now, due),
            Patch(2, "Left Lower", now, due),
            Patch(3, "Right Upper", now, due),
            Patch(4, "Right Lower", null, null),
        )
        val afterRemove = patches.map {
            if (it.id == 2) it.copy(appliedAt = null, dueAt = null) else it
        }
        assertEquals(due, afterRemove.find { it.id == 1 }!!.dueAt)
        assertEquals(due, afterRemove.find { it.id == 3 }!!.dueAt)
        assertEquals(null, afterRemove.find { it.id == 2 }!!.dueAt)
        val suggested = suggestLocations(afterRemove, count = 2)
        assertTrue("removed slot should be suggested", 2 in suggested)
        assertTrue("never-used slot should still be suggested", 4 in suggested)
    }

    @Test
    fun `PATCH_DURATION_MS is exactly 7 days`() {
        assertEquals(604_800_000L, PATCH_DURATION_MS)
    }
}
