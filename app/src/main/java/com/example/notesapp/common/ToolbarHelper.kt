package com.example.notesapp.common

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import com.example.notesapp.search.SearchActivity
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.databinding.ToolBarLayoutBinding
import com.example.notesapp.note.NoteViewModel

object ToolbarHelper {
    fun setup(
        activity: Activity,
        toolbarBinding: ToolBarLayoutBinding,
        viewModel: NoteViewModel?,
        title: String,
        onClickNewFolder: (() -> Unit)? = null,
        onClickSort: () -> Unit

    ) {
        // Set toolbar title
        toolbarBinding.tvTopBarTitle.text = title
        toolbarBinding.tvTopBarTitle.setTypeface(null, Typeface.NORMAL)

        // Search icon click
        toolbarBinding.ivSearch.setOnClickListener {
            activity.startActivity(Intent(activity, SearchActivity::class.java))
        }

        // New folder icon click
        toolbarBinding.ivNewFolder.setOnClickListener {
            onClickNewFolder?.invoke()
        }

        // 3-dots menu (if viewModel is provided)
        viewModel?.let { vm ->
            toolbarBinding.ivThreeDots.setOnClickListener { anchor ->
                PopupMenuWindow(
                    context = activity,
                    anchor = anchor,
                    currentViewMode = vm.viewMode.value ?: ViewMode.LIST,
                    onClickSort = {
                        SortBottomSheet { sortBy, order ->
                            vm.sortNotes(sortBy, order)
                        }.show((activity as androidx.fragment.app.FragmentActivity).supportFragmentManager, "SortBottomSheet")
                    },
                    onClickViewModeChange = { newMode ->
                        vm.setViewMode(newMode)
                    }
                ).show()
            }
        }
    }
}
