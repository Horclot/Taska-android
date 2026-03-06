package com.horclotapp.taska.spaces.model

import com.google.firebase.Timestamp

data class SpaceMember(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val roleId: String = "owner",
    val joinedAt: Timestamp? = null,
    val invitedBy: String? = null,
    val lastActive: Timestamp? = null
)
