package com.example.notesapp.note

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.common.Helper.formatDate
import com.example.notesapp.databinding.ItemNoteBinding

class NoteAdapter(
    private val onItemClick: (NoteEntity) -> Unit,
    private val onNoteLongClick: (NoteEntity) -> Unit
) : ListAdapter<NoteEntity, NoteAdapter.ViewHolder>(DiffCallback) {

    var isMultiSelectMode: Boolean = false
    var selectedIds: Set<Int> = emptySet()
    var onSelectionToggle: ((NoteEntity) -> Unit)? = null

    var onSelectionChanged: ((selectedCount: Int) -> Unit)? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val note = getItem(position)
        holder.bind(note)

        val base64Image = extractFirstImageBase64(note.description)
        if (base64Image != null) {
            val pureBase64 = base64Image.substringAfter("base64,")
            val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)

            val options = BitmapFactory.Options().apply {
                inSampleSize = 2
            }

            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
            holder.binding.ivImageIndicator.setImageBitmap(bitmap)
            holder.binding.ivImageIndicator.visibility = View.VISIBLE
        } else {
            holder.binding.ivImageIndicator.visibility = View.GONE
        }
    }

    inner class ViewHolder(val binding: ItemNoteBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(note: NoteEntity) = with(binding) {
            title.text = getDisplayTitle(note)

            val createdDate = formatDate(note.createdDate)
            val modifiedDate = formatDate(note.modifiedDate)
            val desc = note.description?.lineSequence()?.firstOrNull()?.take(100)?.trim() ?: ""
            val dateText = if (note.isEdited) "$modifiedDate (edited)" else createdDate

            dateAndDesc.text = if (desc.isNotEmpty()) {
                "$dateText · $desc"
            } else {
                dateText
            }

            val isSelected = selectedIds.contains(note.id)
            ivSelector.visibility = if (isMultiSelectMode) View.VISIBLE else View.GONE
            ivSelector.setImageResource(
                if (isSelected) R.drawable.ic_check_circle else R.drawable.ic_circle
            )

            root.background = root.context.getDrawable(
                if (isSelected) R.drawable.bg_note_selected else R.drawable.bg_note_unselected
            )

            root.setOnClickListener {
                if (isMultiSelectMode) {
                    onSelectionToggle?.invoke(note)
                } else {
                    onItemClick(note)
                }
            }

            root.setOnLongClickListener {
                if (!isMultiSelectMode) {
                    isMultiSelectMode = true
                    onSelectionToggle?.invoke(note)
                }
                onNoteLongClick(note)
                true
            }
        }
    }

    fun extractFirstImageBase64(description: String?): String? {
        if (description == null) return null
        val regex = Regex("""\[img\]\s*(data:image\/[^;]+;base64,[A-Za-z0-9+/=]+)""")
        return regex.find(description)?.groupValues?.get(1)
    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<NoteEntity>() {
            override fun areItemsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: NoteEntity, newItem: NoteEntity) =
                oldItem == newItem
        }

        private fun getDisplayTitle(note: NoteEntity): String {
            return if (note.title.isNotBlank()) {
                note.title
            } else {
                note.description?.lineSequence()?.firstOrNull()?.take(100) ?: ""
            }
        }

    }
}
