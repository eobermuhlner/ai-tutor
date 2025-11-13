package ch.obermuhlner.aitutor.email.service

import ch.obermuhlner.aitutor.email.config.EmailProperties
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Service
class EmailService(
    private val mailSender: JavaMailSender?,
    private val templateEngine: TemplateEngine,
    private val emailProperties: EmailProperties
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Send an email using a Thymeleaf template.
     * This method is async to avoid blocking the main thread.
     */
    @Async
    fun sendTemplatedEmail(
        to: String,
        subject: String,
        templateName: String,
        templateVariables: Map<String, Any>
    ) {
        try {
            if (mailSender == null) {
                logger.warn("Email sender not configured. Email to {} with subject '{}' not sent", to, subject)
                return
            }

            val context = Context().apply {
                setVariables(templateVariables)
            }

            val htmlContent = templateEngine.process("email/$templateName", context)

            val message: MimeMessage = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(emailProperties.from, emailProperties.fromName)
            helper.setTo(to)
            helper.setSubject(subject)
            helper.setText(htmlContent, true) // true = HTML

            mailSender.send(message)

            logger.info("Email sent successfully to {} with subject '{}'", to, subject)
        } catch (e: Exception) {
            logger.error("Failed to send email to {} with subject '{}': {}", to, subject, e.message, e)
            // In a production system, you might want to:
            // - Retry the email
            // - Queue it for later
            // - Alert administrators
        }
    }

    /**
     * Send email verification email.
     */
    fun sendVerificationEmail(to: String, username: String, verificationToken: String) {
        val verificationUrl = "${emailProperties.baseUrl}/verify-email?token=$verificationToken"

        val variables = mapOf(
            "username" to username,
            "verificationUrl" to verificationUrl,
            "expirationHours" to emailProperties.verificationTokenExpirationHours
        )

        sendTemplatedEmail(
            to = to,
            subject = "Verify Your Email Address",
            templateName = "email-verification",
            templateVariables = variables
        )
    }

    /**
     * Send password reset email.
     */
    fun sendPasswordResetEmail(to: String, username: String, resetToken: String) {
        val resetUrl = "${emailProperties.baseUrl}/reset-password?token=$resetToken"

        val variables = mapOf(
            "username" to username,
            "resetUrl" to resetUrl,
            "expirationHours" to emailProperties.passwordResetTokenExpirationHours
        )

        sendTemplatedEmail(
            to = to,
            subject = "Reset Your Password",
            templateName = "password-reset",
            templateVariables = variables
        )
    }

    /**
     * Send account locked notification email.
     */
    fun sendAccountLockedEmail(to: String, username: String, unlockTime: String) {
        val variables = mapOf(
            "username" to username,
            "unlockTime" to unlockTime
        )

        sendTemplatedEmail(
            to = to,
            subject = "Account Locked - Security Alert",
            templateName = "account-locked",
            templateVariables = variables
        )
    }

    /**
     * Send password changed notification email.
     */
    fun sendPasswordChangedEmail(to: String, username: String) {
        val variables = mapOf(
            "username" to username,
            "supportUrl" to "${emailProperties.baseUrl}/support"
        )

        sendTemplatedEmail(
            to = to,
            subject = "Password Changed - Security Alert",
            templateName = "password-changed",
            templateVariables = variables
        )
    }
}
