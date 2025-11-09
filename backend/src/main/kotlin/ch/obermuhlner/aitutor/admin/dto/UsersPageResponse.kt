package ch.obermuhlner.aitutor.admin.dto

import ch.obermuhlner.aitutor.auth.dto.UserResponse

/**
 * Paginated response for user list.
 * Admin-only operation.
 */
data class UsersPageResponse(
    val users: List<UserResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int
)
