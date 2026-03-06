package com.horclotapp.taska.spaces.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.horclotapp.taska.R
import com.horclotapp.taska.databinding.ItemSpaceRoleBinding
import com.horclotapp.taska.spaces.model.SpaceRole
import com.horclotapp.taska.spaces.util.RoleTemplates

class SpaceRolesAdapter(
    private val onMakeDefault: (SpaceRole) -> Unit,
    private val onEdit: (SpaceRole) -> Unit,
    private val onDelete: (SpaceRole) -> Unit
) : ListAdapter<SpaceRole, SpaceRolesAdapter.RoleViewHolder>(DiffCallback) {

    var defaultRoleId: String? = null
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoleViewHolder {
        val binding = ItemSpaceRoleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RoleViewHolder(binding, onMakeDefault, onEdit, onDelete)
    }

    override fun onBindViewHolder(holder: RoleViewHolder, position: Int) {
        holder.bind(getItem(position), getItem(position).id == defaultRoleId)
    }

    class RoleViewHolder(
        private val binding: ItemSpaceRoleBinding,
        private val onMakeDefault: (SpaceRole) -> Unit,
        private val onEdit: (SpaceRole) -> Unit,
        private val onDelete: (SpaceRole) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(role: SpaceRole, isDefault: Boolean) {
            val context = binding.root.context
            binding.roleName.text = role.name
            binding.roleMeta.text = if (role.isSystem) {
                context.getString(R.string.spaces_roles_meta_system)
            } else {
                context.getString(R.string.spaces_roles_meta_custom)
            }
            binding.defaultBadge.isVisible = isDefault
            binding.systemBadge.isVisible = role.isSystem

            binding.colorPreview.setBackgroundColor(
                runCatching { Color.parseColor(role.color) }.getOrDefault(Color.GRAY)
            )

            binding.defaultButton.text = if (isDefault) {
                context.getString(R.string.spaces_roles_default_badge)
            } else {
                context.getString(R.string.spaces_roles_set_default)
            }
            binding.defaultButton.isEnabled = !isDefault
            binding.defaultButton.setOnClickListener {
                onMakeDefault(role)
            }

            binding.editButton.setOnClickListener {
                onEdit(role)
            }

            binding.deleteButton.visibility = if (role.id == RoleTemplates.OWNER_ROLE_ID) {
                View.GONE
            } else {
                View.VISIBLE
            }
            binding.deleteButton.setOnClickListener {
                onDelete(role)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<SpaceRole>() {
        override fun areItemsTheSame(oldItem: SpaceRole, newItem: SpaceRole): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SpaceRole, newItem: SpaceRole): Boolean {
            return oldItem == newItem
        }
    }
}
