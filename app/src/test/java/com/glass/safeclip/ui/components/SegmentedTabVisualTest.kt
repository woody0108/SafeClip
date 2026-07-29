package com.glass.safeclip.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentedTabVisualTest {
    @Test
    fun selectedTabUsesStrongerContrastThanUnselectedTab() {
        val selected = SegmentedTabVisual.from(selected = true)
        val unselected = SegmentedTabVisual.from(selected = false)

        assertEquals(SegmentedTabTone.Active, selected.tone)
        assertEquals(SegmentedTabTone.Inactive, unselected.tone)
        assertTrue(selected.containerAlpha > unselected.containerAlpha)
        assertTrue(selected.contentAlpha > unselected.contentAlpha)
        assertTrue(selected.boldText)
        assertTrue(!unselected.boldText)
    }
}
