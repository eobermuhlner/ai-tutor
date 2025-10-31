package ch.obermuhlner.aitutor.user.service

import ch.obermuhlner.aitutor.user.domain.UserRole
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.demo")
data class DemoUserProperties(
    var enabled: Boolean = true,
    var username: String = "demo",
    var password: String = "demo",
    var email: String = "demo@localhost"
)

@Component
class DemoUserInitializer(
    private val userService: UserService,
    private val demoUserProperties: DemoUserProperties
) {
    private val logger = LoggerFactory.getLogger(DemoUserInitializer::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun initializeDemoUser() {
        if (!demoUserProperties.enabled) {
            logger.info("Demo user initialization disabled")
            return
        }

        val username = demoUserProperties.username
        if (userService.findByUsername(username) == null) {
            logger.info("Creating demo user: $username")
            val demoUser = ch.obermuhlner.aitutor.user.domain.UserEntity(
                username = username,
                email = demoUserProperties.email,
                passwordHash = demoUserProperties.password,
                roles = mutableSetOf(UserRole.USER)
            )
            userService.createUser(demoUser)
            logger.info("Demo user created successfully: $username")
        } else {
            logger.debug("Demo user already exists: $username")
        }
    }
}
