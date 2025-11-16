package ch.obermuhlner.aitutor.auth.config

import ch.obermuhlner.aitutor.auth.filter.JwtAuthenticationFilter
import ch.obermuhlner.aitutor.auth.handler.OAuth2AuthenticationFailureHandler
import ch.obermuhlner.aitutor.auth.handler.OAuth2AuthenticationSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val corsConfigurationSource: CorsConfigurationSource,
    private val env: Environment,
    private val oAuth2SuccessHandler: OAuth2AuthenticationSuccessHandler,
    private val oAuth2FailureHandler: OAuth2AuthenticationFailureHandler
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            // JWT = stateless, no CSRF tokens
            .csrf { it.disable() }

            // Use the explicit CORS bean (origins/methods/headers driven by properties)
            .cors { it.configurationSource(corsConfigurationSource) }

            // Strict session policy for token-based auth
            .sessionManagement { sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }

            // Route authorization
            .authorizeHttpRequests { auth ->
                auth
                    // Public auth endpoints
                    .requestMatchers(
                        "/api/v1/auth/register",
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh"
                    ).permitAll()

                    // OAuth2 endpoints (Google login)
                    .requestMatchers(
                        "/oauth2/**",
                        "/login/oauth2/**"
                    ).permitAll()

                    // Stripe webhook endpoint (validated via signature)
                    .requestMatchers(
                        "/api/v1/webhooks/stripe"
                    ).permitAll()

                    // Public file/image endpoint you listed
                    .requestMatchers(
                        "/api/v1/images/concept/*/data"
                    ).permitAll()

                    // OpenAPI / Swagger (consider gating these to dev only in production)
                    .requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/swagger-ui.html",
                        "/swagger-ui/**",
                        "/webjars/**"
                    ).permitAll()

                    // Actuator endpoints (health, info, metrics)
                    .requestMatchers(
                        "/actuator",
                        "/actuator/**"
                    ).permitAll()

                    // All API requires auth
                    .requestMatchers(
                        "/api/v1/**"
                    ).authenticated()

                    // Everything else denied by default
                    .anyRequest().denyAll()
            }

            // OAuth2 login configuration - only apply to non-API endpoints
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(oAuth2SuccessHandler)
                    .failureHandler(oAuth2FailureHandler)
            }

            // Add JWT filter ahead of UsernamePasswordAuthenticationFilter
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        // Security headers
        http.headers { headers ->
            // Content-Type sniffing protection
            headers.contentTypeOptions { }

            // HSTS only when not running in dev (assumes TLS in prod)
            if (!env.acceptsProfiles(Profiles.of("dev"))) {
                headers.httpStrictTransportSecurity { hsts ->
                    hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)
                }
                // In prod, deny framing entirely
                headers.frameOptions { it.deny() }
            } else {
                // Dev: allow H2 console frames
                headers.frameOptions { it.sameOrigin() }
            }

            // Optional minimal CSP for APIs (safe default; adjust if you serve HTML here)
            headers.contentSecurityPolicy { csp ->
                csp.policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none';")
            }
        }

        return http.build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder(12)

    @Bean
    fun authenticationManager(cfg: AuthenticationConfiguration): AuthenticationManager =
        cfg.authenticationManager
}
