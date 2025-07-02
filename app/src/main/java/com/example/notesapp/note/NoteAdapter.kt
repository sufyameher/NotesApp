package com.example.notesapp.note

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.common.Helper.formatDate
import com.example.notesapp.databinding.ItemNoteBinding

class NoteAdapter(
    private val onItemClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteAdapter.ViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) = with(binding) {
            title.text = note.title

            val createdDate = formatDate(note.createdDate)
            val modifiedDate = formatDate(note.modifiedDate)
            val desc = note.description.trim()
            val dateText = if (note.isEdited) "$modifiedDate (edited)" else createdDate

            dateAndDesc.text = if (desc.isNotEmpty()) {
                "$dateText · $desc"
            } else {
                dateText
            }

            root.setOnClickListener { onItemClick(note) }
            root.setOnLongClickListener {
                onNoteLongClick(note)
                true
            }
        }
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NoteEntity>() {
            override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
                oldItem == newItem
        }
    }
}
