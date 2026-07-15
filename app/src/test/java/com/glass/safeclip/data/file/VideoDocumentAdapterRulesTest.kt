package com.glass.safeclip.data.file

import org.junit.Assert.assertEquals
import org.junit.Test

class VideoDocumentAdapterRulesTest {
    @Test
    fun safeDisplayNameUsesFallbackWhenNameIsBlank() {
        assertEquals("선택한 폴더", VideoDocumentAdapterRules.safeDisplayName(null, "선택한 폴더"))
        assertEquals("선택한 폴더", VideoDocumentAdapterRules.safeDisplayName("", "선택한 폴더"))
        assertEquals("EVENT", VideoDocumentAdapterRules.safeDisplayName("EVENT", "선택한 폴더"))
    }
}
