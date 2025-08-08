package com.example.notesapp.folder.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.common.onClick
import com.example.notesapp.common.onLongClick
import com.example.notesapp.common.setVisible
import com.example.notesapp.databinding.ItemFolderWithInfoBinding
import com.example.notesapp.folder.model.FolderWithNoteCount

class FolderWithInfoAdapter(
    private var folders: List<FolderWithNoteCount>,
    private val onFolderClick: (FolderWithNoteCount) -> Unit,
    private val onFolderLongClick: (FolderWithNoteCount) -> Unit
) : RecyclerView.Adapter<FolderWithInfoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFolderWithInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size

    inner class ViewHolder(
        private val binding: ItemFolderWithInfoBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FolderWithNoteCount) = with(binding) {
            val (folder, subfolderCount, noteCount) = item
            tvFolderName.text = folder.name

            val info = when {
                subfolderCount > 0 && noteCount > 0 ->
                    "$subfolderCount folders · $noteCount notes"
                subfolderCount > 0 ->
                    "$subfolderCount folders"
                noteCount > 0 ->
                    "$noteCount notes"
                else -> ""
            }

            tvFolderInfo.text = info
            tvFolderInfo.setVisible(info.isNotEmpty())

            root.onClick { onFolderClick(item) }
            root.onLongClick { onFolderLongClick(item)
                true
            }
        }
    }

    fun updateFolders(newFolders: List<FolderWithNoteCount>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}