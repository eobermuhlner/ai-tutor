package ch.obermuhlner.aitutor.testharness.scenario

import ch.obermuhlner.aitutor.testharness.domain.*
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
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

    private val objectMapper = ObjectMapper(YAMLFactory())
        .registerModule(KotlinModule.Builder().build())
        .findAndRegisterModules() // This registers JavaTimeModule and other modules

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
                        e.printStackTrace()
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
            description = this.description ?: "",
            language = this.learnerPersona?.targetLanguage ?: "en",
            level = this.learnerPersona?.cefrLevel ?: "A1",
            topic = this.learnerPersona?.learningGoals?.firstOrNull() ?: "General",
            objective = this.description ?: "",
            learnerPersona = LearnerPersona(
                name = this.learnerPersona?.name ?: "Default Learner",
                level = this.learnerPersona?.cefrLevel ?: "A1",
                learningStyle = "Balanced",
                goals = this.learnerPersona?.learningGoals ?: emptyList(),
                commonMistakes = this.learnerPersona?.commonErrors ?: emptyList(),
                personality = "Engaged"
            ),
            expectedBehaviors = this.evaluationFocus ?: emptyList(),
            testSteps = this.conversationScript?.mapIndexed { index, step ->
                TestStep(
                    stepNumber = index + 1,
                    action = "learner_speaks",
                    expectedOutcome = step.content, // Use the actual content from the conversation script
                    evaluationCriteria = emptyList()
                )
            } ?: emptyList()
        )
    }
}

// YAML data class structure that matches the actual scenario files
@JsonIgnoreProperties(ignoreUnknown = true)
data class ScenarioYaml(
    val id: String?,
    val name: String,
    val description: String?,
    val learnerPersona: LearnerPersonaYaml?,
    val tutorConfig: TutorConfigYaml?,
    val conversationScript: List<ConversationScriptYaml>?,
    val expectedOutcomes: ExpectedOutcomesYaml?,
    val evaluationFocus: List<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LearnerPersonaYaml(
    val name: String,
    val cefrLevel: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val commonErrors: List<String>?,
    val learningGoals: List<String>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TutorConfigYaml(
    val tutorName: String,
    val initialPhase: String,
    val teachingStyle: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ConversationScriptYaml(
    val content: String,
    val intentionalErrors: List<IntentionalErrorYaml>?,
    val notes: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class IntentionalErrorYaml(
    val span: String,
    val errorType: String,
    val expectedSeverity: String,
    val correctForm: String,
    val reasoning: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class ExpectedOutcomesYaml(
    val phaseTransitions: List<PhaseTransitionYaml>?,
    val minimumCorrectionsDetected: Int,
    val shouldTriggerDrillPhase: Boolean?,
    val shouldMaintainFreePhase: Boolean?,
    val topicChanges: Int?,
    // Allow unknown properties for fields like vocabularyItems, etc.
    val additionalProperties: Map<String, Any>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PhaseTransitionYaml(
    val afterMessageIndex: Int,
    val toPhase: String,
    val reason: String
)