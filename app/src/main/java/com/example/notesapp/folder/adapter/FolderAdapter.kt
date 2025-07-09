package com.example.notesapp.folder.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.databinding.ItemFolderBinding
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.note.NoteEntity

class FolderAdapter(
    private var folders: List<FolderEntity>,
    private val onFolderClick: (FolderEntity) -> Unit,
    private val onFolderLongClick: (FolderEntity) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    var isMultiSelectMode: Boolean = false
    var selectedIds: Set<Int> = emptySet()
    var onSelectionToggle: ((FolderEntity) -> Unit)? = null
    var onSelectionChanged: ((selectedCount: Int) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size

    fun updateFolders(newFolders: List<FolderEntity>) {
        folders = newFolders
        notifyDataSetChanged()
    }

    inner class FolderViewHolder(private val binding: ItemFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: FolderEntity) = with(binding) {
            tvFolderName.text = folder.name
            ivFolderIcon.setImageResource(R.drawable.ic_folder)

            val isSelected = selectedIds.contains(folder.id)
            ivSelector.apply {
                visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
                setImageResource(if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_circle)
            }

            root.background = root.context.getDrawable(
                if (isSelected) R.drawable.bg_note_selected else R.drawable.bg_note_unselected
            )

            root.setOnClickListener {
                if (isMultiSelectMode) onSelectionToggle?.invoke(folder)
                else onFolderClick(folder)
            }

            root.setOnLongClickListener {
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