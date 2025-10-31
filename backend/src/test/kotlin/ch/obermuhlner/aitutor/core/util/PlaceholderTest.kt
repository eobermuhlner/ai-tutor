package ch.obermuhlner.aitutor.core.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlaceholderTest {

    @Test
    fun `substitute simple placeholder`() {
        val template = "Hello \${NAME}"
        val values = mapOf("NAME" to "Alice")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello Alice", result)
    }

    @Test
    fun `substitute multiple placeholders`() {
        val template = "Hello \${NAME}, you are \${AGE} years old"
        val values = mapOf("NAME" to "Bob", "AGE" to "25")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello Bob, you are 25 years old", result)
    }

    @Test
    fun `substitute repeated placeholders`() {
        val template = "\${NAME} is \${NAME}"
        val values = mapOf("NAME" to "Alice")

        val result = Placeholder.substitute(template, values)
        assertEquals("Alice is Alice", result)
    }

    @Test
    fun `substitute with no placeholders`() {
        val template = "Hello world"
        val values = mapOf("NAME" to "Alice")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello world", result)
    }

    @Test
    fun `substitute with missing values leaves placeholder`() {
        val template = "Hello \${NAME} and \${FRIEND}"
        val values = mapOf("NAME" to "Alice")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello Alice and \${FRIEND}", result)
    }

    @Test
    fun `substitute with empty map leaves placeholders`() {
        val template = "Hello \${NAME}"
        val values = emptyMap<String, String>()

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello \${NAME}", result)
    }

    @Test
    fun `substitute handles special characters in values`() {
        val template = "Hello \${NAME}"
        val values = mapOf("NAME" to "Alice & Bob")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello Alice & Bob", result)
    }

    @Test
    fun `substitute handles underscores in keys`() {
        val template = "Hello \${FIRST_NAME}"
        val values = mapOf("FIRST_NAME" to "Alice")

        val result = Placeholder.substitute(template, values)
        assertEquals("Hello Alice", result)
    }

    @Test
    fun `substitute handles numbers in keys`() {
        val template = "Age: \${AGE1}, Count: \${COUNT2}"
        val values = mapOf("AGE1" to "30", "COUNT2" to "5")

        val result = Placeholder.substitute(template, values)
        assertEquals("Age: 30, Count: 5", result)
    }

    @Test
    fun `substitute with lowercase key does not match`() {
        val template = "Hello \${name}"
        val values = mapOf("NAME" to "Alice")

        // lowercase keys won't match the uppercase pattern
        val result = Placeholder.substitute(template, values)
        assertEquals("Hello \${name}", result)
    }
}
