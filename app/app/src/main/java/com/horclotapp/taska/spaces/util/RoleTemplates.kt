package com.horclotapp.taska.spaces.util

import com.google.firebase.Timestamp
import com.horclotapp.taska.spaces.model.SpaceRole

object RoleTemplates {
    const val OWNER_ROLE_ID = "owner"
    const val MANAGER_ROLE_ID = "manager"
    const val DEVELOPER_ROLE_ID = "developer"
    const val DESIGNER_ROLE_ID = "designer"
    const val VIEWER_ROLE_ID = "viewer"

    fun defaultRoles(now: Timestamp): List<Pair<String, SpaceRole>> {
        return listOf(
            OWNER_ROLE_ID to SpaceRole(
                id = OWNER_ROLE_ID,
                name = "Owner",
                color = "#FF6B6B",
                createdAt = now,
                isSystem = true
            ),
            MANAGER_ROLE_ID to SpaceRole(
                id = MANAGER_ROLE_ID,
                name = "Manager",
                color = "#FFC857",
                createdAt = now,
                isSystem = true
            ),
            DEVELOPER_ROLE_ID to SpaceRole(
                id = DEVELOPER_ROLE_ID,
                name = "Developer",
                color = "#FF5722",
                createdAt = now,
                isSystem = true
            ),
            DESIGNER_ROLE_ID to SpaceRole(
                id = DESIGNER_ROLE_ID,
                name = "Designer",
                color = "#00BCD4",
                createdAt = now,
                isSystem = true
            ),
            VIEWER_ROLE_ID to SpaceRole(
                id = VIEWER_ROLE_ID,
                name = "Viewer",
                color = "#9EA2B0",
                createdAt = now,
                isSystem = true
            )
        )
    }
}
