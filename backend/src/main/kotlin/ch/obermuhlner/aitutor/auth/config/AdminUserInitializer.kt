package ch.obermuhlner.aitutor.auth.config

import ch.obermuhlner.aitutor.user.domain.UserEntity
import ch.obermuhlner.aitutor.user.domain.UserRole
import ch.obermuhlner.aitutor.user.service.UserService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class AdminUserInitializer(
    private val userService: UserService,
    @Value("\${app.admin.username:admin}") private val adminUsername: String,
    @Value("\${app.admin.password:}") private val adminPassword: String,
    @Value("\${app.admin.email:admin@localhost}") private val adminEmail: String
) : ApplicationRunner {

    private val logger = LoggerFactory.getLogger(AdminUserInitializer::class.java)

    override fun run(args: ApplicationArguments) {
        // Check if admin user already exists
        if (userService.existsByUsername(adminUsername)) {
            logger.info("Admin user '{}' already exists, skipping initialization", adminUsername)
            return
        }

        // Validate admin password is provided
        if (adminPassword.isBlank()) {
            logger.warn("ADMIN_PASSWORD environment variable not set. Admin user will not be created.")
            logger.warn("Set ADMIN_PASSWORD environment variable and restart to create admin user.")
            return
        }

        // Validate admin password strength
        if (adminPassword.length < 8) {
            logger.error("Admin password must be at least 8 characters long. Admin user not created.")
            return
        }

        try {
            // Create admin user
            val adminUser = UserEntity(
                username = adminUsername,
                email = adminEmail,
                passwordHash = adminPassword,  // Will be hashed by UserService
                firstName = "System",
                lastName = "Administrator",
                roles = mutableSetOf(UserRole.USER, UserRole.ADMIN),
                enabled = true,
                emailVerified = true
            )

            userService.createUser(adminUser)
            logger.info("✓ Admin user '{}' created successfully with email '{}'", adminUsername, adminEmail)
            logger.info("✓ Admin roles: [USER, ADMIN]")
        } catch (e: Exception) {
            logger.error("Failed to create admin user: {}", e.message, e)
        }
    }
}
