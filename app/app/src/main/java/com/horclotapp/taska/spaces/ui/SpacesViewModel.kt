package com.horclotapp.taska.spaces.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.horclotapp.taska.spaces.data.SpacesFirestoreService
import com.horclotapp.taska.spaces.data.SpacesRepository
import com.horclotapp.taska.spaces.model.SpaceRole
import com.horclotapp.taska.spaces.model.UserSpaceSummary
import kotlinx.coroutines.launch

class SpacesViewModel : ViewModel() {

    private val repository = SpacesRepository(
        auth = FirebaseAuth.getInstance(),
        service = SpacesFirestoreService(FirebaseFirestore.getInstance())
    )

    private val _state = MutableLiveData(SpacesUiState())
    val state: LiveData<SpacesUiState> = _state

    fun loadSpaces() {
        _state.value = _state.value?.copy(
            isListLoading = true,
            errorMessage = null,
            warningMessage = null
        )

        viewModelScope.launch {
            repository.loadUserSpaces()
                .onSuccess { result ->
                    _state.value = _state.value?.copy(
                        isListLoading = false,
                        spaces = result.spaces,
                        warningMessage = result.warningMessage
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value?.copy(
                        isListLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить пространства"
                    )
                }
        }
    }

    fun openRolesSetup(spaceId: String, spaceName: String) {
        _state.value = _state.value?.copy(
            isRolesSetupVisible = true,
            rolesSetupSpaceId = spaceId,
            rolesSetupSpaceName = spaceName,
            errorMessage = null,
            successMessage = null,
            warningMessage = null
        )
        loadRolesSetup(spaceId)
    }

    fun closeRolesSetup() {
        _state.value = _state.value?.copy(
            isRolesSetupVisible = false,
            isRolesLoading = false,
            rolesSetupSpaceId = null,
            rolesSetupSpaceName = null,
            roles = emptyList(),
            defaultRoleId = null,
            errorMessage = null,
            successMessage = null
        )
    }

    fun loadRolesSetup(spaceId: String? = null) {
        val resolvedSpaceId = spaceId ?: _state.value?.rolesSetupSpaceId ?: return

        _state.value = _state.value?.copy(
            isRolesLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            repository.loadRolesSetup(resolvedSpaceId)
                .onSuccess { result ->
                    _state.value = _state.value?.copy(
                        isRolesLoading = false,
                        rolesSetupSpaceId = result.spaceId,
                        rolesSetupSpaceName = result.spaceName,
                        defaultRoleId = result.defaultRoleId,
                        roles = result.roles
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value?.copy(
                        isRolesLoading = false,
                        errorMessage = error.message ?: "Не удалось загрузить роли"
                    )
                }
        }
    }

    fun toggleCreateForm() {
        _state.value = _state.value?.copy(
            isCreateFormVisible = !(_state.value?.isCreateFormVisible ?: false),
            errorMessage = null,
            warningMessage = null
        )
    }

    fun createSpace(name: String, description: String) {
        if (_state.value?.isSaving == true) return

        _state.value = _state.value?.copy(
            isSaving = true,
            errorMessage = null,
            successMessage = null,
            warningMessage = null
        )

        viewModelScope.launch {
            repository.createSpace(name, description)
                .onSuccess { spaceId ->
                    _state.value = _state.value?.copy(
                        isSaving = false,
                        createdSpaceId = spaceId,
                        successMessage = null,
                        isCreateFormVisible = false
                    )
                    loadSpaces()
                    openRolesSetup(spaceId, name.trim())
                }
                .onFailure { error ->
                    _state.value = _state.value?.copy(
                        isSaving = false,
                        errorMessage = error.message ?: "Не удалось создать пространство"
                    )
                }
        }
    }

    fun createRole(
        name: String,
        color: String
    ) {
        val spaceId = _state.value?.rolesSetupSpaceId ?: return
        updateRoleStateLoading(true)

        viewModelScope.launch {
            repository.createRole(spaceId, name, color)
                .onSuccess {
                    updateRoleStateLoading(false, "Роль создана")
                    loadRolesSetup(spaceId)
                }
                .onFailure { error ->
                    updateRoleStateLoading(false, errorMessage = error.message ?: "Не удалось создать роль")
                }
        }
    }

    fun updateRole(
        roleId: String,
        name: String,
        color: String
    ) {
        val spaceId = _state.value?.rolesSetupSpaceId ?: return
        updateRoleStateLoading(true)

        viewModelScope.launch {
            repository.updateRole(spaceId, roleId, name, color)
                .onSuccess {
                    updateRoleStateLoading(false, "Роль обновлена")
                    loadRolesSetup(spaceId)
                }
                .onFailure { error ->
                    updateRoleStateLoading(false, errorMessage = error.message ?: "Не удалось обновить роль")
                }
        }
    }

    fun deleteRole(roleId: String) {
        val spaceId = _state.value?.rolesSetupSpaceId ?: return
        updateRoleStateLoading(true)

        viewModelScope.launch {
            repository.deleteRole(spaceId, roleId)
                .onSuccess {
                    updateRoleStateLoading(false, "Роль удалена")
                    loadRolesSetup(spaceId)
                }
                .onFailure { error ->
                    updateRoleStateLoading(false, errorMessage = error.message ?: "Не удалось удалить роль")
                }
        }
    }

    fun updateDefaultRole(roleId: String) {
        val spaceId = _state.value?.rolesSetupSpaceId ?: return
        updateRoleStateLoading(true)

        viewModelScope.launch {
            repository.updateDefaultRole(spaceId, roleId)
                .onSuccess {
                    updateRoleStateLoading(false, "Роль по умолчанию обновлена")
                    loadRolesSetup(spaceId)
                }
                .onFailure { error ->
                    updateRoleStateLoading(false, errorMessage = error.message ?: "Не удалось обновить роль по умолчанию")
                }
        }
    }

    private fun updateRoleStateLoading(
        isLoading: Boolean,
        successMessage: String? = null,
        errorMessage: String? = null
    ) {
        _state.value = _state.value?.copy(
            isRoleSaving = isLoading,
            successMessage = successMessage,
            errorMessage = errorMessage
        )
    }
}

data class SpacesUiState(
    val isListLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isRolesLoading: Boolean = false,
    val isRoleSaving: Boolean = false,
    val isCreateFormVisible: Boolean = false,
    val isRolesSetupVisible: Boolean = false,
    val spaces: List<UserSpaceSummary> = emptyList(),
    val roles: List<SpaceRole> = emptyList(),
    val rolesSetupSpaceId: String? = null,
    val rolesSetupSpaceName: String? = null,
    val defaultRoleId: String? = null,
    val createdSpaceId: String? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val warningMessage: String? = null
)
