package com.horclotapp.taska.spaces.model

data class UserSpaceSummary(
    val space: Space = Space(),
    val accessRoleId: String = "",
    val isOwner: Boolean = false
)
