package ch.obermuhlner.aitutor.testharness.ai

/**
 * Abstraction for AI providers used in the test harness.
 * Supports OpenAI, Azure OpenAI, and Ollama.
 */
interface AiProvider {
    /**
     * Call the AI model with a prompt and return the response.
     *
     * @param prompt The input prompt
     * @param model The model to use (e.g., "gpt-4o", "llama3")
     * @param temperature Temperature for sampling (0.0-2.0)
     * @return The model's text response
     */
    fun chat(prompt: String, model: String, temperature: Double): String
}

/**
 * Configuration for AI providers.
 */
data class AiProviderConfig(
    val type: AiProviderType,
    val apiKey: String? = null,
    val apiEndpoint: String? = null,
    val deploymentName: String? = null // For Azure OpenAI
)

enum class AiProviderType {
    OPENAI,
    AZURE_OPENAI,
    OLLAMA
}
