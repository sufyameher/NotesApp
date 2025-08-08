package com.example.notesapp.folder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.FolderDiffCallback
import com.example.notesapp.R
import com.example.notesapp.common.onClick
import com.example.notesapp.common.onLongClick
import com.example.notesapp.common.setImageResourceBy
import com.example.notesapp.common.setVisible
import com.example.notesapp.databinding.ItemFolderBinding
import com.example.notesapp.folder.model.FolderEntity

class FolderAdapter(
    private val onFolderClick: (FolderEntity) -> Unit,
    private val onFolderLongClick: (FolderEntity) -> Unit
) : ListAdapter<FolderEntity, FolderAdapter.FolderViewHolder>(FolderDiffCallback()) {

    var isMultiSelectMode: Boolean = false
    var selectedIds: Set<Int> = emptySet()
    var onSelectionToggle: ((FolderEntity) -> Unit)? = null
    var onSelectionChanged: ((selectedCount: Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FolderViewHolder(private val binding: ItemFolderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(folder: FolderEntity) = with(binding) {
            tvFolderName.text = folder.name
            ivFolderIcon.setImageResource(R.drawable.ic_folder)

            val selected = selectedIds.contains(folder.id)

            ivSelector.apply {
                setVisible(isMultiSelectMode)
                setImageResourceBy(selected, R.drawable.ic_check_circle, R.drawable.ic_circle)
            }

            root.setBackgroundResource(if (selected) R.drawable.bg_note_selected else R.drawable.bg_note_unselected)

            root.onClick() {
                if (isMultiSelectMode) onSelectionToggle?.invoke(folder)
                else onFolderClick(folder)
            }

            root.onLongClick {
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                    onSelectionToggle?.invoke(folder)
                }
                onFolderLongClick(folder)
                true
            }
        }
    }
}
