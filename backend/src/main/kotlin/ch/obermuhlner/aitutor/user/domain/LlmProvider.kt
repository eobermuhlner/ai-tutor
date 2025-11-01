package ch.obermuhlner.aitutor.user.domain

/**
 * Enum representing supported LLM (Large Language Model) providers.
 */
enum class LlmProvider {
    /**
     * OpenAI API (GPT models)
     */
    OPENAI,

    /**
     * Azure OpenAI Service
     */
    AZURE_OPENAI,

    /**
     * Anthropic API (Claude models)
     */
    ANTHROPIC,

    /**
     * Use system default provider (configured via environment variables)
     */
    SYSTEM_DEFAULT
}
