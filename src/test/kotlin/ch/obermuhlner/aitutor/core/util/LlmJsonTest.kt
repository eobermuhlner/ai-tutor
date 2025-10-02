package ch.obermuhlner.aitutor.core.util

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LlmJsonTest {

    data class TestObject(
        val name: String,
        val age: Int
    )

    @Test
    fun `extractJsonBlock with json fence`() {
        val input = """
            Here is the JSON:
            ```json
            {"name": "Alice", "age": 30}
            ```
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)
        assertEquals("""{"name": "Alice", "age": 30}""", result)
    }

    @Test
    fun `extractJsonBlock with generic fence`() {
        val input = """
            Here is the data:
            ```
            {"name": "Bob", "age": 25}
            ```
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)
        assertEquals("""{"name": "Bob", "age": 25}""", result)
    }

    @Test
    fun `extractJsonBlock with no fence returns null`() {
        val input = """{"name": "Charlie", "age": 35}"""

        val result = LlmJson.extractJsonBlock(input)
        assertNull(result)
    }

    @Test
    fun `extractJsonBlock prefers json fence over generic`() {
        val input = """
            ```
            {"generic": true}
            ```
            ```json
            {"specific": true}
            ```
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)
        assertEquals("""{"specific": true}""", result)
    }

    @Test
    fun `parseNode parses valid JSON block`() {
        val input = """
            ```json
            {"name": "Alice", "age": 30}
            ```
        """.trimIndent()

        val result = LlmJson.parseNode(input)
        assertNotNull(result)
        assertEquals("Alice", result?.get("name")?.asText())
        assertEquals(30, result?.get("age")?.asInt())
    }

    @Test
    fun `parseNode returns null for invalid JSON`() {
        val input = """
            ```json
            {invalid json}
            ```
        """.trimIndent()

        val result = LlmJson.parseNode(input)
        assertNull(result)
    }

    @Test
    fun `parseNode returns null when no fence found`() {
        val input = """{"name": "Alice"}"""

        val result = LlmJson.parseNode(input)
        assertNull(result)
    }

    @Test
    fun `parseAs deserializes to target type`() {
        val input = """
            ```json
            {"name": "Alice", "age": 30}
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestObject>(input)
        assertNotNull(result)
        assertEquals("Alice", result?.name)
        assertEquals(30, result?.age)
    }

    @Test
    fun `parseAs returns null for invalid JSON`() {
        val input = """
            ```json
            {invalid}
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestObject>(input)
        assertNull(result)
    }

    @Test
    fun `parseAs returns null for null input`() {
        val result = LlmJson.parseAs<TestObject>(null)
        assertNull(result)
    }

    @Test
    fun `parseAs handles lenient JSON features`() {
        val input = """
            ```json
            {
                // Comment
                name: 'Alice',  // unquoted key, single quotes
                age: 30,        // trailing comma
            }
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestObject>(input)
        assertNotNull(result)
        assertEquals("Alice", result?.name)
        assertEquals(30, result?.age)
    }

    @Test
    fun `parseAs ignores unknown properties`() {
        val input = """
            ```json
            {"name": "Alice", "age": 30, "extra": "ignored"}
            ```
        """.trimIndent()

        val result = LlmJson.parseAs<TestObject>(input)
        assertNotNull(result)
        assertEquals("Alice", result?.name)
        assertEquals(30, result?.age)
    }

    @Test
    fun `mapper handles missing values in arrays`() {
        val input = """[1,,3]"""

        val result = LlmJson.mapper.readTree(input)
        assertEquals(3, result.size())
        assertEquals(1, result[0].asInt())
        assertTrue(result[1].isNull)
        assertEquals(3, result[2].asInt())
    }

    @Test
    fun `extractJsonBlock handles multiline JSON`() {
        val input = """
            ```json
            {
                "name": "Alice",
                "age": 30,
                "address": {
                    "city": "NYC",
                    "zip": "10001"
                }
            }
            ```
        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)
        assertNotNull(result)
        assertTrue(result!!.contains("Alice"))
        assertTrue(result.contains("NYC"))
    }

    @Test
    fun `extractJsonBlock trims whitespace`() {
        val input = """

            ```json
                {"name": "Alice"}
            ```

        """.trimIndent()

        val result = LlmJson.extractJsonBlock(input)
        assertEquals("""{"name": "Alice"}""", result?.trim())
    }
}
