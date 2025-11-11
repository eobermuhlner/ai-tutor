package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.CurriculumRuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CurriculumRuleRepository : JpaRepository<CurriculumRuleEntity, UUID> {
    fun findByCourseId(courseId: UUID): CurriculumRuleEntity?
    fun existsByCourseId(courseId: UUID): Boolean
    fun deleteByCourseId(courseId: UUID)
}