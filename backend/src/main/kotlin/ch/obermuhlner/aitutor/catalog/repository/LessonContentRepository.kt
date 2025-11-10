package ch.obermuhlner.aitutor.catalog.repository

import ch.obermuhlner.aitutor.catalog.domain.LessonContentEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LessonContentRepository : JpaRepository<LessonContentEntity, UUID> {
    fun findByCourseId(courseId: UUID): List<LessonContentEntity>
    fun findByCourseIdAndLessonId(courseId: UUID, lessonId: String): LessonContentEntity?
    fun deleteByCourseId(courseId: UUID)
    fun existsByCourseIdAndLessonId(courseId: UUID, lessonId: String): Boolean
}