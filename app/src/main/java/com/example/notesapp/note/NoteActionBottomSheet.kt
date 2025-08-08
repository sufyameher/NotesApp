package com.example.notesapp.note

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.example.notesapp.R
import com.example.notesapp.common.Helper.formatDate
import com.example.notesapp.common.gone
import com.example.notesapp.common.onClick
import com.example.notesapp.common.onClickAndDismiss
import com.example.notesapp.common.setPinIcon
import com.example.notesapp.common.setPinText
import com.example.notesapp.databinding.BottomSheetNoteActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NoteActionBottomSheet(
    private val note: NoteEntity,
    private val onMoveTo: (NoteEntity) -> Unit,
    private val onCopyTo: (NoteEntity) -> Unit,
    private val onDelete: (NoteEntity) -> Unit,
    private var onTogglePin: (NoteEntity) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetNoteActionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetNoteActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupNoteInfo()
        setupClickListeners()
    }

    private fun setupNoteInfo() = with(binding) {
        optionRecover.gone()
        tvNoteTitle.text = note.title
        tvNoteDate.text = formatDate(note.createdDate)
        tvPinText.setPinText(note.isPinned)
        ivPinIcon.setPinIcon(note.isPinned)
    }

    private fun setupClickListeners() = with(binding) {
        optionMove.onClickAndDismiss(note, this@NoteActionBottomSheet, onMoveTo)
        optionCopy.onClickAndDismiss(note, this@NoteActionBottomSheet, onCopyTo)
        optionDelete.onClickAndDismiss(note, this@NoteActionBottomSheet, onDelete)
        optionPin.onClickAndDismiss(note, this@NoteActionBottomSheet, onTogglePin)}

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = null
    }
}
