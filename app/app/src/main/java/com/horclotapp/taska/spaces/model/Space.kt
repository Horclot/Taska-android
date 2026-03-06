package com.horclotapp.taska.spaces.model

import com.google.firebase.Timestamp

data class Space(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val ownerId: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null,
    val settings: SpaceSettings = SpaceSettings(),
    val stats: SpaceStats = SpaceStats()
)

data class SpaceSettings(
    val defaultRoleId: String? = null
)

data class SpaceStats(
    val members: Int = 1,
    val nodes: Int = 0
)
