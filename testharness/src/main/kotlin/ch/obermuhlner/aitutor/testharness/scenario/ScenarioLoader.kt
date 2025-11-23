package ch.obermuhlner.aitutor.testharness.scenario

import ch.obermuhlner.aitutor.testharness.domain.*
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import java.io.File

/**
 * Loads test scenarios from YAML files
 */
@Service
class ScenarioLoader {
    
    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    
    /**
     * Load a single scenario from a YAML file
     */
    fun loadScenarioFromFile(filePath: String): TestScenario {
        val file = File(filePath)
        val yamlContent = if (file.exists()) {
            file.readText()
        } else {
            // Try to load from classpath
            ClassPathResource(filePath).inputStream.bufferedReader().use { it.readText() }
        }
        
        return objectMapper.readValue(yamlContent, ScenarioYaml::class.java).toDomain()
    }
    
    /**
     * Load all scenarios from a directory
     */
    fun loadScenariosFromDirectory(directoryPath: String): List<TestScenario> {
        val dir = File(directoryPath)
        val scenarios = mutableListOf<TestScenario>()
        
        if (dir.exists() && dir.isDirectory) {
            dir.walkTopDown()
                .filter { it.extension.equals("yml", ignoreCase = true) || it.extension.equals("yaml", ignoreCase = true) }
                .forEach { file ->
                    try {
                        val yamlContent = file.readText()
                        val scenarioYaml = objectMapper.readValue(yamlContent, ScenarioYaml::class.java)
                        scenarios.add(scenarioYaml.toDomain())
                    } catch (e: Exception) {
                        println("Error loading scenario from ${file.absolutePath}: ${e.message}")
                    }
                }
        }
        
        return scenarios
    }
    
    /**
     * Helper function to convert YAML model to domain model
     */
    private fun ScenarioYaml.toDomain(): TestScenario {
        return TestScenario(
            id = this.id ?: "scenario_${System.currentTimeMillis()}",
            name = this.name,
            description = this.description,
            language = this.language,
            level = this.level,
            topic = this.topic,
            objective = this.objective,
            learnerPersona = LearnerPersona(
                name = this.learnerPersona?.name ?: "Default Learner",
                level = this.learnerPersona?.level ?: "Intermediate",
                learningStyle = this.learnerPersona?.learningStyle ?: "Balanced",
                goals = this.learnerPersona?.goals ?: emptyList(),
                commonMistakes = this.learnerPersona?.commonMistakes ?: emptyList(),
                personality = this.learnerPersona?.personality ?: "Engaged"
            ),
            expectedBehaviors = this.expectedBehaviors ?: emptyList(),
            testSteps = this.testSteps?.mapIndexed { index, step ->
                TestStep(
                    stepNumber = index + 1,
                    action = step.action,
                    expectedOutcome = step.expectedOutcome ?: "",
                    evaluationCriteria = step.evaluationCriteria ?: emptyList()
                )
            } ?: emptyList()
        )
    }
}

// YAML data class structure
data class ScenarioYaml(
    val id: String?,
    val name: String,
    val description: String,
    val language: String,
    val level: String,
    val topic: String,
    val objective: String,
    val learnerPersona: LearnerPersonaYaml?,
    val expectedBehaviors: List<String>?,
    val testSteps: List<TestStepYaml>?
)

data class LearnerPersonaYaml(
    val name: String,
    val level: String,
    val learningStyle: String,
    val goals: List<String>,
    val commonMistakes: List<String>,
    val personality: String
)

data class TestStepYaml(
    val action: String,
    val expectedOutcome: String?,
    val evaluationCriteria: List<String>?
)