package com.domnis.nebuni.ui.theme

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TypeTest {

    @Test
    fun latinLanguagesUseTheBundledFonts() {
        listOf("en", "fr", "de", "it", "es").forEach { language ->
            assertTrue(usesBundledFonts(language), "$language should use the bundled fonts")
        }
    }

    @Test
    fun japaneseUsesThePlatformFont() {
        assertFalse(usesBundledFonts("ja"))
        assertFalse(usesBundledFonts("JA"))
    }
}
