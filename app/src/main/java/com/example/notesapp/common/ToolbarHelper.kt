package com.example.notesapp.common

import android.app.Activity
import android.graphics.Typeface
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.example.notesapp.search.SearchActivity
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.databinding.ToolBarLayoutBinding
import com.example.notesapp.note.MainActivityViewModel
import kotlinx.coroutines.launch

object ToolbarHelper {
    fun setup(
        activity: Activity,
        toolbarBinding: ToolBarLayoutBinding,
        viewModel: MainActivityViewModel?,
        title: String,
        onClickNewFolder: (() -> Unit)? = null,
        onClickSort: () -> Unit

    ) {
        // Set toolbar title
        toolbarBinding.tvTopBarTitle.text = title
        toolbarBinding.tvTopBarTitle.setTypeface(null, Typeface.NORMAL)

        // Search icon click
        toolbarBinding.ivSearch.setOnClickListener {
            SearchActivity.start(activity)
        }

        // New folder icon click
        toolbarBinding.ivNewFolder.setOnClickListener {
            onClickNewFolder?.invoke()
        }

        // 3-dots menu (if viewModel is provided)
        viewModel?.let { mainActivityViewModel ->
            toolbarBinding.ivThreeDots.setOnClickListener { anchor ->
                PopupMenuWindow(
                    context = activity,
                    anchor = anchor,
                    currentViewMode = mainActivityViewModel.viewMode.value ?: ViewMode.LIST,
                    onClickSort = {
                        SortBottomSheet.show((activity as FragmentActivity).supportFragmentManager) { sortBy, order ->
                            mainActivityViewModel.sortNotes(viewModel.rootFolderId, sortBy, order)
                        }
                    },
                    onClickViewModeChange = { newMode ->
                        mainActivityViewModel.setViewMode(newMode)
                    },
                    onClickAddDummyData = {
                        (activity as? FragmentActivity)?.lifecycleScope?.launch {
                            val existingNotes = mainActivityViewModel.getAllNotesOnce()
                            if (existingNotes.isNotEmpty()) {
                                Toast.makeText(activity, "Dummy data already added", Toast.LENGTH_SHORT).show()
                            } else {
                                mainActivityViewModel.createDummyNotesAndFolders()
                            }
                        }
                    }
                ).show()
            }
        }
    }
}
