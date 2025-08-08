package com.example.notesapp.common.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.notesapp.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.example.notesapp.common.PreferenceUtil
import com.example.notesapp.common.SortBy
import com.example.notesapp.common.SortOrder
import com.example.notesapp.databinding.BottomSheetSortBinding

class SortBottomSheet(
    private val onSortSelected: (sortBy: String, order: String) -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var binding: BottomSheetSortBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetSortBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
         when (PreferenceUtil.sortBy) {
            SortBy.DATE_CREATED -> binding.rbDateCreated.isChecked = true
            SortBy.DATE_EDITED -> binding.rbDateEdited.isChecked = true
            SortBy.TITLE -> binding.rbTitle.isChecked = true
        }

        updateOrderLabels(binding.rgSortType.checkedRadioButtonId)

        when (PreferenceUtil.sortOrder) {
            SortOrder.ASCENDING, SortOrder.A_TO_Z -> binding.rbOldToNew.isChecked = true
            SortOrder.DESCENDING, SortOrder.Z_TO_A -> binding.rbNewToOld.isChecked = true
        }

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
            R.id.rbDateCreated -> SortBy.DATE_CREATED
            R.id.rbDateEdited -> SortBy.DATE_EDITED
            R.id.rbTitle -> SortBy.TITLE
            else -> SortBy.DATE_CREATED
        }

        val sortOrder = when (binding.rgSortOrder.checkedRadioButtonId) {
            R.id.rbOldToNew -> when (sortBy) {
                SortBy.TITLE -> SortOrder.A_TO_Z
                else -> SortOrder.ASCENDING
            }
            R.id.rbNewToOld -> when (sortBy) {
                SortBy.TITLE -> SortOrder.Z_TO_A
                else -> SortOrder.DESCENDING
            }
            else -> SortOrder.ASCENDING
        }

         PreferenceUtil.sortBy = sortBy
         PreferenceUtil.sortOrder = sortOrder
        onSortSelected(sortBy.name, sortOrder.name)
    }

    companion object {
        fun show(
            fragmentManager: androidx.fragment.app.FragmentManager,
            onSortSelected: (sortBy: String, order: String) -> Unit
        ) {
            SortBottomSheet(onSortSelected).show(fragmentManager, "SortBottomSheet")
        }
    }
}
