package com.example.s_journal.architecture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Automated guardrail tests that enforce non-negotiable project and architecture rules
 * defined in AGENT.md (zero network permissions, backup disabled, i18n parity between
 * English and Spanish, zero Java files in main, and Room schema export integrity).
 */
class ProjectGuardrailsTest {

    private fun resolveAppDir(): File {
        val currentDir = File(System.getProperty("user.dir") ?: ".")
        return if (currentDir.name == "app" && File(currentDir, "src/main").exists()) {
            currentDir
        } else {
            File(currentDir, "app")
        }
    }

    @Test
    fun manifest_neverRequestsInternetPermission_and_disablesBackup() {
        val manifestFile = File(resolveAppDir(), "src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml must exist at ${manifestFile.absolutePath}", manifestFile.exists())

        val content = manifestFile.readText()

        assertFalse(
            "Main branch must NEVER declare INTERNET permission (AGENT.md Rule #5)",
            content.contains("android.permission.INTERNET")
        )
        assertFalse(
            "Main branch must NEVER declare ACCESS_NETWORK_STATE permission",
            content.contains("android.permission.ACCESS_NETWORK_STATE")
        )
        assertFalse(
            "App must not request legacy external storage permissions",
            content.contains("READ_EXTERNAL_STORAGE") || content.contains("WRITE_EXTERNAL_STORAGE")
        )
        assertTrue(
            "AndroidManifest.xml must explicitly set android:allowBackup=\"false\" to protect private journal entries",
            content.contains("android:allowBackup=\"false\"")
        )
    }

    @Test
    fun stringsXml_englishAndSpanish_haveExactSameKeysAndFormatArgs() {
        val enFile = File(resolveAppDir(), "src/main/res/values/strings.xml")
        val esFile = File(resolveAppDir(), "src/main/res/values-es/strings.xml")

        assertTrue("Default strings.xml must exist", enFile.exists())
        assertTrue("Spanish strings.xml must exist", esFile.exists())

        val enStrings = parseStringsXml(enFile)
        val esStrings = parseStringsXml(esFile)

        val missingInEs = enStrings.keys - esStrings.keys
        val extraInEs = esStrings.keys - enStrings.keys

        assertTrue(
            "Spanish strings.xml is missing keys present in English: $missingInEs",
            missingInEs.isEmpty()
        )
        assertTrue(
            "Spanish strings.xml has extra keys not present in English: $extraInEs",
            extraInEs.isEmpty()
        )

        val formatSpecifierRegex = Regex("%(\\d+\\$)?[-#+ 0,(]*\\d*(\\.\\d+)?[bBhHsScCdoxXeEfgGaAtT%]")

        for ((key, enValue) in enStrings) {
            val esValue = esStrings[key].orEmpty()
            assertTrue("English string '$key' must not be blank", enValue.isNotBlank())
            assertTrue("Spanish string '$key' must not be blank", esValue.isNotBlank())

            val enPlaceholders = formatSpecifierRegex.findAll(enValue).map { it.value }.toList()
            val esPlaceholders = formatSpecifierRegex.findAll(esValue).map { it.value }.toList()

            assertEquals(
                "Format placeholders mismatch for string key '$key' between EN and ES",
                enPlaceholders,
                esPlaceholders
            )
        }
    }

    @Test
    fun sourceCode_containsZeroJavaFilesInMain() {
        val mainJavaDir = File(resolveAppDir(), "src/main/java")
        assertTrue("src/main/java directory must exist", mainJavaDir.exists())

        val javaFiles = mainJavaDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("java", ignoreCase = true) }
            .map { it.relativeTo(mainJavaDir).path }
            .toList()

        assertTrue(
            "AGENT.md Rule #1 violation: 100% Kotlin required, but found Java files: $javaFiles",
            javaFiles.isEmpty()
        )
    }

    @Test
    fun roomSchema_isExportedAndContainsDateIndex() {
        val schemaFile = File(
            resolveAppDir(),
            "schemas/com.example.s_journal.data.local.database.JournalDatabase/1.json"
        )
        assertTrue("Room exported schema 1.json must exist", schemaFile.exists())

        val schemaJson = schemaFile.readText()
        assertTrue("Room schema must define journal_entries table", schemaJson.contains("\"tableName\": \"journal_entries\""))
        assertTrue("Room schema must define index on 'date' column", schemaJson.contains("index_journal_entries_date"))
    }

    private fun parseStringsXml(file: File): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        doc.documentElement.normalize()
        val nodes = doc.getElementsByTagName("string")
        val result = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i)
            val nameAttr = element.attributes?.getNamedItem("name")?.nodeValue
            if (!nameAttr.isNullOrBlank()) {
                result[nameAttr] = element.textContent ?: ""
            }
        }
        return result
    }
}
