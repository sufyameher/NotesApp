package com.example.notesapp.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notesapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.notesapp.databinding.BottomSheetSortBinding

class SortBottomSheet(
    private val onSortSelected: (sortBy: String, order: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetSortBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = BottomSheetSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.rbDateCreated.isChecked = true
        updateOrderLabels(R.id.rbDateCreated)
        binding.rbOldToNew.isChecked = true

        binding.rgSortType.setOnCheckedChangeListener { _, checkedId ->
            updateOrderLabels(checkedId)
            binding.rbOldToNew.isChecked = true
            triggerSortCallback()
        }

        binding.rgSortOrder.setOnCheckedChangeListener { _, _ ->
            triggerSortCallback()
        }
    }

    private fun updateOrderLabels(checkedId: Int) {
        when (checkedId) {
            R.id.rbTitle -> {
                binding.rbOldToNew.text = "A - Z"
                binding.rbNewToOld.text = "Z - A"
            }
            else -> {
                binding.rbOldToNew.text = "Old to New"
                binding.rbNewToOld.text = "New to Old"
            }
        }
    }

    private fun triggerSortCallback() {
        val sortBy = when (binding.rgSortType.checkedRadioButtonId) {
            R.id.rbDateCreated -> "DATE_CREATED"
            R.id.rbDateEdited -> "DATE_EDITED"
            R.id.rbTitle -> "TITLE"
            else -> "DATE_CREATED"
        }

        val order = when (binding.rgSortOrder.checkedRadioButtonId) {
            R.id.rbOldToNew -> "ASC"
            R.id.rbNewToOld -> "DESC"
            else -> "ASC"
        }

        onSortSelected(sortBy, order)
    }
}
