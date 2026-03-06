package com.horclotapp.taska

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.horclotapp.taska.databinding.DialogRoleEditorBinding
import com.horclotapp.taska.databinding.FragmentSpacesBinding
import com.horclotapp.taska.spaces.model.SpaceRole
import com.horclotapp.taska.spaces.ui.SpaceRolesAdapter
import com.horclotapp.taska.spaces.ui.SpacesAdapter
import com.horclotapp.taska.spaces.ui.SpacesViewModel

class SpacesFragment : Fragment(R.layout.fragment_spaces) {

    private var _binding: FragmentSpacesBinding? = null
    private val binding get() = _binding!!
    private val viewModel by viewModels<SpacesViewModel>()

    private val spacesAdapter = SpacesAdapter { summary ->
        viewModel.openRolesSetup(summary.space.id, summary.space.name)
    }

    private val rolesAdapter = SpaceRolesAdapter(
        onMakeDefault = { role -> viewModel.updateDefaultRole(role.id) },
        onEdit = { role -> showRoleEditorDialog(role) },
        onDelete = { role -> showDeleteRoleDialog(role) }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSpacesBinding.bind(view)

        setupLists()
        setupButtons()
        observeState()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadSpaces()
        viewModel.loadRolesSetup()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupLists() {
        binding.spacesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.spacesRecyclerView.adapter = spacesAdapter

        binding.rolesRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.rolesRecyclerView.adapter = rolesAdapter
    }

    private fun setupButtons() {
        binding.openCreateButton.setOnClickListener {
            viewModel.toggleCreateForm()
        }

        binding.createButton.setOnClickListener {
            binding.nameInputLayout.error = null
            binding.descriptionInputLayout.error = null

            val name = binding.nameEditText.text?.toString().orEmpty()
            val description = binding.descriptionEditText.text?.toString().orEmpty()

            if (name.trim().length < 3) {
                binding.nameInputLayout.error = "Минимум 3 символа"
                return@setOnClickListener
            }

            if (description.trim().length > 300) {
                binding.descriptionInputLayout.error = "Максимум 300 символов"
                return@setOnClickListener
            }

            viewModel.createSpace(name, description)
        }

        binding.addRoleButton.setOnClickListener {
            showRoleEditorDialog(null)
        }

        binding.closeRolesButton.setOnClickListener {
            viewModel.closeRolesSetup()
        }
    }

    private fun observeState() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            val isListMode = !state.isRolesSetupVisible

            binding.spacesOverviewCard.isVisible = isListMode
            binding.createFormCard.isVisible = isListMode && state.isCreateFormVisible
            binding.listProgressBar.isVisible = isListMode && state.isListLoading
            binding.emptyStateCard.isVisible = isListMode && !state.isListLoading && state.spaces.isEmpty()
            binding.spacesRecyclerView.isVisible = isListMode && state.spaces.isNotEmpty()

            binding.createButton.isEnabled = !state.isSaving
            binding.progressBar.isVisible = state.isSaving
            binding.openCreateButton.text = if (state.isCreateFormVisible) {
                getString(R.string.spaces_hide_create_button)
            } else {
                getString(R.string.spaces_open_create_button)
            }

            binding.rolesSetupCard.isVisible = state.isRolesSetupVisible
            binding.rolesProgressBar.isVisible = state.isRolesLoading || state.isRoleSaving
            binding.rolesRecyclerView.isVisible = state.isRolesSetupVisible && state.roles.isNotEmpty()
            binding.rolesEmptyState.isVisible = state.isRolesSetupVisible && !state.isRolesLoading && state.roles.isEmpty()
            binding.addRoleButton.isEnabled = !state.isRoleSaving
            binding.closeRolesButton.isEnabled = !state.isRoleSaving

            binding.spacesCount.text = resources.getQuantityString(
                R.plurals.spaces_count,
                state.spaces.size,
                state.spaces.size
            )

            binding.rolesSpaceTitle.text = state.rolesSetupSpaceName ?: getString(R.string.spaces_roles_title)
            binding.rolesCount.text = resources.getQuantityString(
                R.plurals.roles_count,
                state.roles.size,
                state.roles.size
            )
            binding.defaultRoleValue.text = state.roles.firstOrNull {
                it.id == state.defaultRoleId
            }?.name ?: getString(R.string.spaces_roles_default_not_set)

            spacesAdapter.submitList(state.spaces)
            rolesAdapter.defaultRoleId = state.defaultRoleId
            rolesAdapter.submitList(state.roles)

            binding.statusCard.isVisible = !state.errorMessage.isNullOrBlank() ||
                !state.successMessage.isNullOrBlank() ||
                !state.warningMessage.isNullOrBlank()

            when {
                !state.errorMessage.isNullOrBlank() -> {
                    binding.statusTitle.text = getString(R.string.status_error)
                    binding.statusMessage.text = state.errorMessage
                }

                !state.successMessage.isNullOrBlank() -> {
                    binding.statusTitle.text = getString(R.string.status_success)
                    binding.statusMessage.text = state.successMessage
                }

                !state.warningMessage.isNullOrBlank() -> {
                    binding.statusTitle.text = getString(R.string.status_warning)
                    binding.statusMessage.text = state.warningMessage
                }

                else -> {
                    binding.statusCard.isVisible = false
                }
            }
        }
    }

    private fun showRoleEditorDialog(role: SpaceRole?) {
        val dialogBinding = DialogRoleEditorBinding.inflate(LayoutInflater.from(requireContext()))
        dialogBinding.roleNameEditText.setText(role?.name.orEmpty())
        dialogBinding.roleColorEditText.setText(role?.color ?: "#FF5722")
        bindColorPreset(dialogBinding.colorRose, dialogBinding, "#FF6B6B")
        bindColorPreset(dialogBinding.colorAmber, dialogBinding, "#FFC857")
        bindColorPreset(dialogBinding.colorOrange, dialogBinding, "#FF5722")
        bindColorPreset(dialogBinding.colorTeal, dialogBinding, "#00BCD4")
        bindColorPreset(dialogBinding.colorBlue, dialogBinding, "#4D7CFE")
        bindColorPreset(dialogBinding.colorSlate, dialogBinding, "#9EA2B0")

        val title = if (role == null) {
            getString(R.string.spaces_roles_create_role)
        } else {
            getString(R.string.spaces_roles_edit_role)
        }

        val positiveText = if (role == null) {
            getString(R.string.spaces_roles_create_action)
        } else {
            getString(R.string.spaces_roles_save_action)
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(dialogBinding.root)
            .setNegativeButton(R.string.spaces_roles_cancel_action, null)
            .setPositiveButton(positiveText, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                dialogBinding.roleNameInputLayout.error = null
                dialogBinding.roleColorInputLayout.error = null

                val name = dialogBinding.roleNameEditText.text?.toString().orEmpty().trim()
                val color = dialogBinding.roleColorEditText.text?.toString().orEmpty().trim()

                when {
                    name.length < 2 -> dialogBinding.roleNameInputLayout.error = "Минимум 2 символа"
                    !Regex("^#([A-Fa-f0-9]{6})$").matches(color) ->
                        dialogBinding.roleColorInputLayout.error = "Формат #RRGGBB"
                    else -> {
                        if (role == null) {
                            viewModel.createRole(name, color)
                        } else {
                            viewModel.updateRole(role.id, name, color)
                        }
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun bindColorPreset(view: View, dialogBinding: DialogRoleEditorBinding, color: String) {
        view.setOnClickListener {
            dialogBinding.roleColorEditText.setText(color)
        }
    }

    private fun showDeleteRoleDialog(role: SpaceRole) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.spaces_roles_delete_title)
            .setMessage(getString(R.string.spaces_roles_delete_message, role.name))
            .setNegativeButton(R.string.spaces_roles_cancel_action, null)
            .setPositiveButton(R.string.spaces_roles_delete_action) { _, _ ->
                viewModel.deleteRole(role.id)
            }
            .show()
    }
}
