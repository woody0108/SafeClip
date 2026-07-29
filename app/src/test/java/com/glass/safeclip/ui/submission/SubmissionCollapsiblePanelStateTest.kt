package com.glass.safeclip.ui.submission

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionCollapsiblePanelStateTest {
    @Test
    fun expandedPanelShowsContentAndCollapseLabel() {
        val state = SubmissionCollapsiblePanelState(expanded = true)

        assertTrue(state.showContent)
        assertEquals("최소화", state.toggleLabel)
    }

    @Test
    fun collapsedPanelShowsOnlyTitleAndExpandLabel() {
        val state = SubmissionCollapsiblePanelState(expanded = false)

        assertFalse(state.showContent)
        assertEquals("최대화", state.toggleLabel)
    }

    @Test
    fun toggleSwitchesExpandedState() {
        val state = SubmissionCollapsiblePanelState(expanded = true)

        assertFalse(state.toggle().expanded)
    }
}
