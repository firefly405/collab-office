package com.collaboffice.core.common

object PermissionHelper {
    
    // Roles definitions
    const val ROLE_OWNER = "owner"
    const val ROLE_ADMIN = "admin"
    const val ROLE_MEMBER = "member"
    const val ROLE_GUEST = "guest"

    // Actions/Permissions definitions
    const val PERM_READ = "read"
    const val PERM_WRITE = "write"
    const val PERM_DELETE = "delete"
    const val PERM_ADMIN = "admin"

    /**
     * Check if a role can perform an action based on space permission scheme
     */
    fun hasSpacePermission(role: String, permission: String): Boolean {
        return when (role) {
            ROLE_OWNER -> true
            ROLE_ADMIN -> true
            ROLE_MEMBER -> {
                permission == PERM_READ || permission == PERM_WRITE
            }
            ROLE_GUEST -> {
                permission == PERM_READ
            }
            else -> false
        }
    }

    /**
     * Check if a role can edit/delete/manage a document
     */
    fun hasDocumentPermission(role: String, permission: String, isCreator: Boolean): Boolean {
        if (isCreator) return true
        return when (role) {
            ROLE_OWNER -> true
            ROLE_ADMIN -> true
            ROLE_MEMBER -> {
                permission == PERM_READ || permission == PERM_WRITE
            }
            ROLE_GUEST -> {
                permission == PERM_READ
            }
            else -> false
        }
    }
}
