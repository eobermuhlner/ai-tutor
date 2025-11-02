package ch.obermuhlner.aitutor.core.util

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class LlmJsonTest {

    @Test
    fun `extractJsonBlock should extract valid JSON from markdown block`() {
        val input = """
            Some text here
            ```json
            {
                "name": "test",
                "value": 123
            }
            ```
            More text here
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.contains("\"name\": \"test\""))
        Assertions.assertTrue(result.contains("\"value\": 123"))
    }

    @Test
    fun `extractJsonBlock should extract JSON from any fenced block when json tag not present`() {
        val input = """
            Some text here
            ```
            {
                "name": "test"
            }
            ```
            More text
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.contains("\"name\": \"test\""))
    }

    @Test
    fun `extractJsonBlock should return null when no fenced block found`() {
        val input = """{"name": "test"}"""

        val result = LlmJson.extractJsonBlock(input)

        Assertions.assertNull(result)
    }

    @Test
    fun `extractJsonBlock should handle smart quotes`() {
        val input = """
            ```json
            {
                "name": "test",
                'value': 123
            }
            ```
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.contains("\"name\": \"test\""))
    }

    @Test
    fun `parseNode should parse valid JSON`() {
        val input = """
            ```json
            {
                "name": "test",
                "value": 123
            }
            ```
        """.trimIndent()

        val result = LlmJson.parseNode(input) as JsonNode?

        Assertions.assertNotNull(result)
        Assertions.assertTrue(result!!.has("name"))
        Assertions.assertTrue(result.has("value"))
        Assertions.assertEquals("test", result.get("name").asText())
        Assertions.assertEquals(123, result.get("value").asInt())
    }

    @Test
    fun `parseNode should return null for invalid JSON`() {
        val input = """
            ```json
            {
                "name": "test",
                "value": 123
            missing_closing_brace
            ```
        """.trimIndent()

        val result = LlmJson.parseNode(input)

        Assertions.assertNull(result)
    }

    @Test
    fun `parseAs should deserialize into target type`() {
        val input = """
            ```json
            {
                "name": "test",
                "value": 123
            }
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestData>(input)

        Assertions.assertNotNull(result)
        Assertions.assertEquals("test", result?.name)
        Assertions.assertEquals(123, result?.value)
    }

    @Test
    fun `parseAs should handle lenient JSON features`() {
        val input = """
            ```json
            {
                name: 'test',  // unquoted field name and single quotes
                value: 123,
            }  // trailing comma
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestData>(input)

        Assertions.assertNotNull(result)
        Assertions.assertEquals("test", result?.name)
        Assertions.assertEquals(123, result?.value)
    }

    @Test
    fun `parseAs should return null when parsing fails`() {
        val input = """
            ```json
            {
                "name": "test",
                "value": "not_a_number"
            }
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestData>(input)

        Assertions.assertNull(result)
    }
    
    data class TestData(val name: String, val value: Int)

    @Test
    fun `parseAs should return null when input is null`() {
        data class TestData(val name: String, val value: Int)

        val result = LlmJson.parseAs<TestData>(null)

        Assertions.assertNull(result)
    }

    @Test
    fun `parseAs should return null when no JSON block found`() {
        data class TestData(val name: String, val value: Int)

        val input = "Just some regular text without JSON blocks"

        val result = LlmJson.parseAs<TestData>(input)

        Assertions.assertNull(result)
    }
}