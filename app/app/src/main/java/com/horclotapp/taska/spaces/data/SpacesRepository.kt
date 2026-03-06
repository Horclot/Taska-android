package com.horclotapp.taska.spaces.data

import com.google.firebase.auth.FirebaseAuth
import com.horclotapp.taska.spaces.model.LoadSpacesResult
import com.horclotapp.taska.spaces.model.RolesSetupData

class SpacesRepository(
    private val auth: FirebaseAuth,
    private val service: SpacesFirestoreService
) {

    suspend fun loadUserSpaces(): Result<LoadSpacesResult> {
        val userId = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))

        return runCatching {
            service.loadUserSpaces(userId)
        }
    }

    suspend fun loadRolesSetup(spaceId: String): Result<RolesSetupData> {
        return runCatching {
            ensureAuthorized()
            service.loadRolesSetup(spaceId)
        }
    }

    suspend fun createRole(
        spaceId: String,
        name: String,
        color: String
    ): Result<Unit> {
        return runCatching {
            ensureAuthorized()
            validateRole(name, color)
            service.createRole(spaceId, name.trim(), color.trim())
        }
    }

    suspend fun updateRole(
        spaceId: String,
        roleId: String,
        name: String,
        color: String
    ): Result<Unit> {
        return runCatching {
            ensureAuthorized()
            validateRole(name, color)
            service.updateRole(spaceId, roleId, name.trim(), color.trim())
        }
    }

    suspend fun deleteRole(spaceId: String, roleId: String): Result<Unit> {
        return runCatching {
            ensureAuthorized()
            service.deleteRole(spaceId, roleId)
        }
    }

    suspend fun updateDefaultRole(spaceId: String, roleId: String): Result<Unit> {
        return runCatching {
            ensureAuthorized()
            service.updateDefaultRole(spaceId, roleId)
        }
    }

    suspend fun createSpace(
        name: String,
        description: String
    ): Result<String> {
        val owner = auth.currentUser
            ?: return Result.failure(IllegalStateException("Пользователь не авторизован"))

        val trimmedName = name.trim()
        val trimmedDescription = description.trim()

        if (trimmedName.length < 3) {
            return Result.failure(IllegalArgumentException("Название должно быть не короче 3 символов"))
        }

        if (trimmedDescription.length > 300) {
            return Result.failure(IllegalArgumentException("Описание не должно превышать 300 символов"))
        }

        return runCatching {
            service.createSpace(
                name = trimmedName,
                description = trimmedDescription,
                owner = owner
            )
        }
    }

    private fun ensureAuthorized() {
        if (auth.currentUser == null) {
            throw IllegalStateException("Пользователь не авторизован")
        }
    }

    private fun validateRole(name: String, color: String) {
        if (name.trim().length < 2) {
            throw IllegalArgumentException("Название роли должно быть не короче 2 символов")
        }

        if (!Regex("^#([A-Fa-f0-9]{6})$").matches(color.trim())) {
            throw IllegalArgumentException("Цвет роли должен быть в формате #RRGGBB")
        }
    }
}
