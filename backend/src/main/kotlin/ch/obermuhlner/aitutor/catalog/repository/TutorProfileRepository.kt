package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.TutorProfileEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface TutorProfileRepository : JpaRepository<TutorProfileEntity, UUID> {
    fun findByIsActiveTrueOrderByDisplayOrder(): List<TutorProfileEntity>
    fun findByTargetLanguageCodeAndIsActiveTrueOrderByDisplayOrder(languageCode: String): List<TutorProfileEntity>

    /**
     * Find tutors visible to a specific user (global tutors + user's own custom tutors)
     * @param languageCode Target language code
     * @param userId User ID to filter custom tutors
     * @return List of visible tutors ordered by display order
     */
    @Query("""
        SELECT t FROM TutorProfileEntity t
        WHERE t.targetLanguageCode = :languageCode
        AND t.isActive = true
        AND (t.isGlobal = true OR t.createdByUserId = :userId)
        ORDER BY t.displayOrder
    """)
    fun findVisibleTutorsForUser(
        @Param("languageCode") languageCode: String,
        @Param("userId") userId: UUID
    ): List<TutorProfileEntity>

    /**
     * Find all global tutors for a language (admin view or unauthenticated)
     */
    fun findByTargetLanguageCodeAndIsActiveTrueAndIsGlobalTrueOrderByDisplayOrder(
        languageCode: String
    ): List<TutorProfileEntity>
}
