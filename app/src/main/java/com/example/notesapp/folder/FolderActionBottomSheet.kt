package com.example.notesapp.folder

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notesapp.databinding.BottomSheetFolderActionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FolderActionBottomSheet(
    private val folder: FolderEntity,
    private val folderInfo: String?,
    private val onMoveTo: (FolderEntity) -> Unit,
    private val onCopyTo: (FolderEntity) -> Unit,
    private val onDelete: (FolderEntity) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFolderActionsBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetFolderActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = null
    }

    private fun setupUI() = with(binding) {
        tvFolderTitle.text = folder.name
        tvFolderInfo.apply {
            text = folderInfo
            visibility = if (folderInfo.isNullOrBlank()) View.GONE else View.VISIBLE
        }
    }

    private fun setupClickListeners() = with(binding) {
        optionMove.setOnClickListener { onMoveTo(folder); dismiss() }
        optionCopy.setOnClickListener { onCopyTo(folder); dismiss() }
        optionDelete.setOnClickListener { onDelete(folder); dismiss()
        }
    }
}
