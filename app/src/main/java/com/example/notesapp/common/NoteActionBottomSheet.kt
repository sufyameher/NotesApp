package com.example.notesapp.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notesapp.common.Helper.formatDate
import com.example.notesapp.databinding.BottomSheetNoteActionsBinding
import com.example.notesapp.note.NoteEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NoteActionBottomSheet(
    private val note: NoteEntity,
    private val onMoveTo: (NoteEntity) -> Unit,
    private val onCopyTo: (NoteEntity) -> Unit,
    private val onDelete: (NoteEntity) -> Unit
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
        optionRecover.visibility = View.GONE
        tvNoteTitle.text = note.title
        tvNoteDate.text = formatDate(note.createdDate)
    }

    private fun setupClickListeners() = with(binding) {
        optionMove.setOnClickListener { onMoveTo(note); dismiss() }
        optionCopy.setOnClickListener { onCopyTo(note); dismiss() }
        optionDelete.setOnClickListener { onDelete(note);dismiss() }
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = null
    }
}
