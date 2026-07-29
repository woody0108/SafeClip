package com.glass.safeclip.ui.submission

data class SubmissionCollapsiblePanelState(
    val expanded: Boolean
) {
    val showContent: Boolean = expanded
    val toggleLabel: String = if (expanded) "최소화" else "최대화"

    fun toggle(): SubmissionCollapsiblePanelState {
        return copy(expanded = !expanded)
    }
}
