package com.horclotapp.taska.spaces.model

data class LoadSpacesResult(
    val spaces: List<UserSpaceSummary> = emptyList(),
    val warningMessage: String? = null
)
