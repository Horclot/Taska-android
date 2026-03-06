package com.horclotapp.taska.spaces.model

import com.google.firebase.Timestamp

data class SpaceRole(
    val id: String = "",
    val name: String = "",
    val color: String = "#9EA2B0",
    val createdAt: Timestamp? = null,
    val isSystem: Boolean = false
)

data class RolesSetupData(
    val spaceId: String = "",
    val spaceName: String = "",
    val defaultRoleId: String? = null,
    val roles: List<SpaceRole> = emptyList()
)
