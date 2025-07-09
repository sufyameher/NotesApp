package com.example.notesapp.folder.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.databinding.ItemFolderWithInfoBinding
import com.example.notesapp.folder.model.FolderWithNoteCount

class FolderWithInfoAdapter(
    private var folders: List<FolderWithNoteCount>,
    private val onFolderClick: (FolderWithNoteCount) -> Unit,
    private val onFolderLongClick: (FolderWithNoteCount) -> Unit
) : RecyclerView.Adapter<FolderWithInfoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemFolderWithInfoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
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
            tvFolderInfo.visibility = if (info.isEmpty()) View.GONE else View.VISIBLE

            root.setOnClickListener { onFolderClick(item) }
            root.setOnLongClickListener {
                onFolderLongClick(item)
                true
            }
        }

        private fun buildInfo(subfolders: Int, notes: Int): String {
            val subfolderText = "$subfolders subfolder${if (subfolders != 1) "s" else ""}"
            val noteText = "$notes note${if (notes != 1) "s" else ""}"
            return "$subfolderText · $noteText"
        }
    }

    fun updateFolders(newFolders: List<FolderWithNoteCount>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}