package com.domnis.nebuni.resources

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Checks that `MainPage.kt` takes every user-facing string from Compose resources: its code (comments
 * excluded) must not contain any string literal.
 */
class MainPageHardCodedStringsTest {

    @Test
    fun mainPageHasNoStringLiteral() {
        val source = File("src/commonMain/kotlin/com/domnis/nebuni/ui/main/MainPage.kt")
        assertTrue(source.isFile, "MainPage.kt not found at ${source.absolutePath}")

        assertEquals(emptyList(), stringLiterals(source.readText()))
    }

    @Test
    fun findsLiteralsInCode() {
        assertEquals(
            listOf("\"Start at: \${date}\"", "\"A \\\"quoted\\\" icon\""),
            stringLiterals("Text(\"Start at: \${date}\")\nIcon(contentDescription = \"A \\\"quoted\\\" icon\")"),
        )
    }

    @Test
    fun ignoresLiteralsInComments() {
        assertEquals(
            emptyList(),
            stringLiterals("// Text(\"a\")\n/* Text(\"b\") */\nText(stringResource(Res.string.c))"),
        )
    }

    private companion object {
        /** Returns the string literals of a Kotlin source, skipping line and block comments. */
        fun stringLiterals(source: String): List<String> {
            val literals = mutableListOf<String>()
            var index = 0
            while (index < source.length) {
                when {
                    source.startsWith("//", index) ->
                        index = source.indexOf('\n', index).let { if (it == -1) source.length else it }

                    source.startsWith("/*", index) ->
                        index = source.indexOf("*/", index + 2).let { if (it == -1) source.length else it + 2 }

                    source[index] == '"' -> {
                        var end = index + 1
                        while (end < source.length && source[end] != '"') {
                            end += if (source[end] == '\\') 2 else 1
                        }
                        end = minOf(end + 1, source.length)
                        literals += source.substring(index, end)
                        index = end
                    }

                    else -> index++
                }
            }
            return literals
        }
    }
}
