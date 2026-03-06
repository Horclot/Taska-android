package com.horclotapp.taska.spaces.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.horclotapp.taska.databinding.ItemSpaceBinding
import com.horclotapp.taska.spaces.model.UserSpaceSummary

class SpacesAdapter(
    private val onSpaceClick: (UserSpaceSummary) -> Unit
) : ListAdapter<UserSpaceSummary, SpacesAdapter.SpaceViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpaceViewHolder {
        val binding = ItemSpaceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SpaceViewHolder(binding, onSpaceClick)
    }

    override fun onBindViewHolder(holder: SpaceViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SpaceViewHolder(
        private val binding: ItemSpaceBinding,
        private val onSpaceClick: (UserSpaceSummary) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: UserSpaceSummary) {
            binding.spaceName.text = item.space.name
            binding.spaceDescription.text = item.space.description.ifBlank {
                "Без описания"
            }
            binding.spaceMeta.text = buildString {
                append(if (item.isOwner) "Владелец" else "Участник")
                if (item.accessRoleId.isNotBlank() && item.accessRoleId != "owner") {
                    append(" • ")
                    append(item.accessRoleId)
                }
            }
            binding.spaceStats.text = "Участников: ${item.space.stats.members} • Узлов: ${item.space.stats.nodes}"
            binding.root.setOnClickListener {
                onSpaceClick(item)
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<UserSpaceSummary>() {
        override fun areItemsTheSame(oldItem: UserSpaceSummary, newItem: UserSpaceSummary): Boolean {
            return oldItem.space.id == newItem.space.id
        }

        override fun areContentsTheSame(oldItem: UserSpaceSummary, newItem: UserSpaceSummary): Boolean {
            return oldItem == newItem
        }
    }
}
