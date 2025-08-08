package com.example.notesapp

import com.example.notesapp.folder.model.FolderEntity


class FolderDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<FolderEntity>() {
    override fun areItemsTheSame(oldItem: FolderEntity, newItem: FolderEntity): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FolderEntity, newItem: FolderEntity): Boolean {
        return oldItem == newItem
    }
}
