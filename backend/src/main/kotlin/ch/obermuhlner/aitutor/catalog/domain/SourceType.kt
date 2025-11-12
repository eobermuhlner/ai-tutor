package ch.obermuhlner.aitutor.catalog.domain

/**
 * Indicates the origin of catalog data (courses, lessons, tutors).
 * Used to track migration from file-based seeding to API-based management.
 */
enum class SourceType {
    /**
     * Legacy data from YAML configuration and startup seeding.
     * Loaded from application.yml and course-content/ files during application startup.
     */
    SEEDED,

    /**
     * Data imported via file upload API (curriculum.yml + markdown files).
     * Batch imported from structured files through REST API endpoints.
     */
    UPLOADED,

    /**
     * Data created directly through UI or API.
     * Created manually using Course Editor or direct REST API calls.
     */
    CREATED
}
