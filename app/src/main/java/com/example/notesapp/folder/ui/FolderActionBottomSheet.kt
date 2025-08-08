package com.example.notesapp.folder.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notesapp.common.onClick
import com.example.notesapp.common.removeBottomSheetBackground
import com.example.notesapp.common.setTextOrGone
import com.example.notesapp.databinding.BottomSheetFolderActionsBinding
import com.example.notesapp.folder.model.FolderEntity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FolderActionBottomSheet(
    private val folder: FolderEntity,
    private val folderInfo: String?,
    private val onMoveTo: (FolderEntity) -> Unit,
    private val onCopyTo: (FolderEntity) -> Unit,
    private val onDelete: (FolderEntity) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetFolderActionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetFolderActionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupUI()
        setupClickListeners()
    }

    override fun onStart() {
        super.onStart()
        removeBottomSheetBackground()
    }

    private fun setupUI() = with(binding) {
        tvFolderTitle.text = folder.name
        tvFolderInfo.apply {
            text = folderInfo
            tvFolderInfo.setTextOrGone(folderInfo)
        }
    }

    private fun setupClickListeners() = with(binding) {
        optionMove.onClick { onMoveTo(folder); dismiss() }
        optionCopy.onClick { onCopyTo(folder); dismiss() }
        optionDelete.onClick { onDelete(folder); dismiss()
        }
    }
}
