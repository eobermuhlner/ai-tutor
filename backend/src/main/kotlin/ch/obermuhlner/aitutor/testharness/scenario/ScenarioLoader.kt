package ch.obermuhlner.aitutor.testharness.scenario

import ch.obermuhlner.aitutor.testharness.domain.TestScenario
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.slf4j.LoggerFactory
import java.io.File

/**
 * Loads test scenarios from YAML files.
 */
class ScenarioLoader(private val scenariosPath: String) {
    private val logger = LoggerFactory.getLogger(ScenarioLoader::class.java)
    private val mapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())

    /**
     * Load all scenarios matching the filter.
     *
     * @param filter "all" for all scenarios, or specific scenario ID/name
     */
    fun loadScenarios(filter: String): List<TestScenario> {
        val scenariosDir = File(scenariosPath)

        if (!scenariosDir.exists() || !scenariosDir.isDirectory) {
            logger.warn("Scenarios directory not found: $scenariosPath")
            return emptyList()
        }

        val yamlFiles = scenariosDir.listFiles { file ->
            file.isFile && (file.extension == "yml" || file.extension == "yaml")
        } ?: emptyArray()

        if (yamlFiles.isEmpty()) {
            logger.warn("No YAML scenario files found in: $scenariosPath")
            return emptyList()
        }

        val allScenarios = yamlFiles.mapNotNull { file ->
            try {
                logger.debug("Loading scenario from: ${file.name}")
                mapper.readValue(file, TestScenario::class.java)
            } catch (e: Exception) {
                logger.error("Failed to load scenario from ${file.name}: ${e.message}", e)
                null
            }
        }

        return if (filter == "all") {
            logger.info("Loaded ${allScenarios.size} scenarios")
            allScenarios
        } else {
            val filtered = allScenarios.filter {
                it.id.contains(filter, ignoreCase = true) ||
                        it.name.contains(filter, ignoreCase = true)
            }
            logger.info("Filtered ${filtered.size} scenarios matching: $filter")
            filtered
        }
    }

    /**
     * Load a single scenario by ID.
     */
    fun loadScenario(scenarioId: String): TestScenario? {
        return loadScenarios("all").find { it.id == scenarioId }
    }
}
