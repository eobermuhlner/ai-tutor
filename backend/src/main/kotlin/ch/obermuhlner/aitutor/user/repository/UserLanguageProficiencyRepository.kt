package ch.obermuhlner.aitutor.user.repository

import ch.obermuhlner.aitutor.core.model.catalog.LanguageProficiencyType
import ch.obermuhlner.aitutor.user.domain.UserLanguageProficiencyEntity
import java.util.UUID
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserLanguageProficiencyRepository : JpaRepository<UserLanguageProficiencyEntity, UUID> {
    fun findByUserIdOrderByIsNativeDescCefrLevelDesc(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndLanguageCode(userId: UUID, languageCode: String): UserLanguageProficiencyEntity?
    fun findByUserIdAndIsNativeTrue(userId: UUID): List<UserLanguageProficiencyEntity>
    fun findByUserIdAndIsPrimaryTrue(userId: UUID): UserLanguageProficiencyEntity?
    fun findByUserIdAndProficiencyType(userId: UUID, type: LanguageProficiencyType): List<UserLanguageProficiencyEntity>
}
