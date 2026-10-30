package com.domnis.nebuni.resources

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Checks that every translated `values-xx/strings.xml` declares exactly the keys of the default
 * `values/strings.xml`, with the same positional placeholders (`%1$s`, `%2$d`, ...) for each key.
 */
class StringResourcesConsistencyTest {

    @Test
    fun everyLanguageMatchesTheDefaultStrings() {
        val resourcesDir = File("src/commonMain/composeResources")
        assertTrue(resourcesDir.isDirectory, "Resources not found at ${resourcesDir.absolutePath}")

        val default = parseStrings(File(resourcesDir, "values/strings.xml").readText())
        val languages = resourcesDir.listFiles { file -> file.isDirectory && file.name.startsWith("values-") }
            .orEmpty()
            .mapNotNull { dir -> File(dir, "strings.xml").takeIf { it.isFile } }
            .sortedBy { it.path }
        assertTrue(languages.isNotEmpty(), "No translated strings.xml found")

        val errors = languages.flatMap { file ->
            consistencyErrors(default, parseStrings(file.readText())).map { "${file.parentFile.name}: $it" }
        }
        if (errors.isNotEmpty()) fail(errors.joinToString("\n"))
    }

    @Test
    fun reportsAMissingKey() {
        val translation = parseStrings(
            """<resources><string name="greeting">Bonjour %1${'$'}s</string></resources>"""
        )

        assertEquals(listOf("missing key 'count'"), consistencyErrors(DEFAULT, translation))
    }

    @Test
    fun reportsAnExtraKey() {
        val translation = parseStrings(
            """
            <resources>
                <string name="greeting">Bonjour %1${'$'}s</string>
                <string name="count">%1${'$'}d éléments</string>
                <string name="extra">En trop</string>
            </resources>
            """
        )

        assertEquals(listOf("unknown key 'extra'"), consistencyErrors(DEFAULT, translation))
    }

    @Test
    fun reportsAMissingPlaceholder() {
        val translation = parseStrings(
            """
            <resources>
                <string name="greeting">Bonjour</string>
                <string name="count">%1${'$'}d éléments</string>
            </resources>
            """
        )

        assertEquals(
            listOf("key 'greeting' has placeholders [] instead of [%1\$s]"),
            consistencyErrors(DEFAULT, translation),
        )
    }

    @Test
    fun reportsAPlaceholderWithAnotherType() {
        val translation = parseStrings(
            """
            <resources>
                <string name="greeting">Bonjour %1${'$'}s</string>
                <string name="count">%1${'$'}s éléments</string>
            </resources>
            """
        )

        assertEquals(
            listOf("key 'count' has placeholders [%1\$s] instead of [%1\$d]"),
            consistencyErrors(DEFAULT, translation),
        )
    }

    @Test
    fun acceptsReorderedPlaceholders() {
        val default = parseStrings("""<resources><string name="ra">RA: %1${'$'}s (%2${'$'}s)</string></resources>""")
        val translation = parseStrings("""<resources><string name="ra">(%2${'$'}s) AD : %1${'$'}s</string></resources>""")

        assertEquals(emptyList(), consistencyErrors(default, translation))
    }

    private companion object {
        // Declared first: DEFAULT below is parsed during companion initialization.
        val PLACEHOLDER = Regex("""%\d+\$[a-zA-Z]""")

        val DEFAULT = parseStrings(
            """
            <resources>
                <string name="greeting">Hello %1${'$'}s</string>
                <string name="count">%1${'$'}d items</string>
            </resources>
            """
        )

        /** Parses a strings.xml content into key -> sorted positional placeholders. */
        fun parseStrings(xml: String): Map<String, List<String>> {
            val document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(xml.trimIndent().byteInputStream())
            val nodes = document.getElementsByTagName("string")
            return (0 until nodes.length).associate { index ->
                val element = nodes.item(index) as Element
                element.getAttribute("name") to
                    PLACEHOLDER.findAll(element.textContent).map { it.value }.distinct().sorted().toList()
            }
        }

        fun consistencyErrors(
            default: Map<String, List<String>>,
            translation: Map<String, List<String>>,
        ): List<String> {
            val missing = (default.keys - translation.keys).map { "missing key '$it'" }
            val unknown = (translation.keys - default.keys).map { "unknown key '$it'" }
            val placeholders = default.keys.intersect(translation.keys)
                .filter { default[it] != translation[it] }
                .map { "key '$it' has placeholders ${translation[it]} instead of ${default[it]}" }
            return missing + unknown + placeholders
        }
    }
}
