package com.example.notesapp.trash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.notesapp.common.Helper.formatDate
import com.example.notesapp.databinding.BottomSheetNoteActionsBinding
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.util.showDeleteForeverConfirmation
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class TrashNoteActionBottomSheet(
    private val note: NoteEntity,
    private val onRecover: (NoteEntity) -> Unit,
    private val onMoveTo: (NoteEntity) -> Unit,
    private val onDeleteForever: (NoteEntity) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetNoteActionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetNoteActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupNoteInfo()
        customizeOptionTexts()
        configureVisibleOptions()
        setupClickListeners()
    }

    private fun setupNoteInfo() = with(binding) {
        tvNoteTitle.text = note.title
        tvNoteDate.text = formatDate(note.createdDate)
    }

    private fun customizeOptionTexts() {
        (binding.optionMove.getChildAt(1) as? TextView)?.text = "Move to"
        (binding.optionDelete.getChildAt(1) as? TextView)?.text = "Delete Forever"

    }

    private fun configureVisibleOptions() = with(binding) {
        optionCopy.visibility = View.GONE
        optionMove.visibility = View.VISIBLE
        optionRecover.visibility = View.VISIBLE
    }

    private fun setupClickListeners() = with(binding) {
        optionMove.setOnClickListener {
            onMoveTo(note)
            dismiss()
        }

        optionDelete.setOnClickListener {
            showDeleteForeverConfirmation(requireContext()) {
                onDeleteForever(note)
                dismiss()
            }
        }

        optionRecover.setOnClickListener {
            onRecover(note)
            dismiss()
        }
    }
}
