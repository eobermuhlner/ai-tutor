package ch.obermuhlner.aitutor.core.util

object Placeholder {
    private val regex = Regex("""\$\{([A-Z0-9_]+)}""")

    fun substitute(source: String, vars: Map<String, String>): String {
        return regex.replace(source) { m ->
            val key = m.groupValues[1]
            vars[key] ?: m.value   // leave untouched if not found
        }
    }
}