package ch.obermuhlner.aitutor.core.util

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.json.JsonReadFeature
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue

object LlmJson {
    private val fencedJsonRegex = Regex(
        pattern = "```\\s*json\\s*\\R(.*?)\\R?```",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val fencedAnyRegex = Regex(
        pattern = "```\\s*\\R(.*?)\\R?```",
        options = setOf(RegexOption.DOT_MATCHES_ALL)
    )

    // Highly lenient mapper
    val mapper: ObjectMapper = ObjectMapper(
        JsonFactory.builder()
            .enable(JsonReadFeature.ALLOW_JAVA_COMMENTS)         // // and /* */ comments
            .enable(JsonReadFeature.ALLOW_SINGLE_QUOTES)         // 'strings'
            .enable(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES)  // unquoted keys
            .enable(JsonReadFeature.ALLOW_TRAILING_COMMA)        // [1,2,]
            .enable(JsonReadFeature.ALLOW_MISSING_VALUES)        // [1,,3]
            .build()
    )
        .registerModule(KotlinModule.Builder().build())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    /**
     * Extract the JSON code block contents from an LLM response.
     * Prefers ```json ... ``` blocks. Falls back to any ``` ... ``` block.
     * If none found, returns null.
     */
    fun extractJsonBlock(source: String): String? {
        val cleaned = source
            .replace("\u201C", "\"").replace("\u201D", "\"") // smart double quotes -> "
            .replace("\u2018", "'").replace("\u2019", "'")   // smart single quotes -> '
            .trim()

        val m1 = fencedJsonRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim()
        if (m1 != null) return m1

        val m2 = fencedAnyRegex.find(cleaned)?.groupValues?.getOrNull(1)?.trim()
        if (m2 != null) return m2

        return null
    }

    /**
     * Parse the extracted JSON into a JsonNode.
     * Returns null if extraction or parsing fails.
     */
    fun parseNode(source: String): JsonNode? {
        val block = extractJsonBlock(source) ?: return null
        return try { mapper.readTree(block) } catch (_: Exception) { null }
    }

    /**
     * Deserialize into a target type with maximal leniency.
     * Returns null on failure.
     */
    inline fun <reified T> parseAs(source: String?): T? {
        if (source == null) return null
        val block = extractJsonBlock(source) ?: return null
        return try { mapper.readValue<T>(block) } catch (_: Exception) { null }
    }
}
