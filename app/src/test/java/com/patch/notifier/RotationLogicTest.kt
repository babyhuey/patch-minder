package com.patch.notifier

import com.patch.notifier.data.Patch
import com.patch.notifier.data.suggestLocations
import org.junit.Assert.assertEquals
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
        assert(4 in suggested) { "Should suggest the unused location" }
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
}
