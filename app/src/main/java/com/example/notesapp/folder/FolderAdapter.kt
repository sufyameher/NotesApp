package com.example.notesapp.folder

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.databinding.ItemFolderBinding

class FolderAdapter(
    private var folders: List<FolderEntity>,
    private val onFolderClick: (FolderEntity) -> Unit,
    private val onFolderLongClick: (FolderEntity) -> Unit
) : RecyclerView.Adapter<FolderAdapter.FolderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FolderViewHolder {
        val binding = ItemFolderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FolderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FolderViewHolder, position: Int) {
        holder.bind(folders[position])
    }

    override fun getItemCount() = folders.size

    inner class FolderViewHolder(
        private val binding: ItemFolderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(folder: FolderEntity) = with(binding) {
            tvFolderName.text = folder.name
            ivFolderIcon.setImageResource(R.drawable.ic_folder)

            root.setOnClickListener { onFolderClick(folder) }
            root.setOnLongClickListener { onFolderLongClick(folder)
                true
            }
        }
    }

    fun updateFolders(newFolders: List<FolderEntity>) {
        folders = newFolders
        notifyDataSetChanged()
    }
}
