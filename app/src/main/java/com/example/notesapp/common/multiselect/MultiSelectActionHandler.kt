 package com.example.notesapp.common.multiselect

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.folder.data.FolderViewModel
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteViewModel
import com.example.notesapp.util.showMultiFolderDialog

class MultiSelectActionHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val noteViewModel: NoteViewModel,
    private val folderViewModel: FolderViewModel,
    private val fragmentManager: FragmentManager,
    private val layoutInflater: LayoutInflater,
    private val exitMultiSelectMode: () -> Unit
) {

    fun handleMove(
        selectedNotes: List<NoteEntity>,
        selectedFolders: List<FolderEntity>
    ) {
        if (selectedNotes.isEmpty() && selectedFolders.isEmpty()) return

        folderViewModel.activeFolders.observeOnce(lifecycleOwner) { allFolders ->
            val excludedIds = selectedFolders.map { it.id }.toSet()

            showMultiFolderDialog(
                context = context,
                inflater = layoutInflater,
                folders = allFolders,
                excludedFolderIds = excludedIds,
                titleText = "Move to",
                startFromFolder = null
            ) { targetFolder ->
                targetFolder?.let {
                    selectedNotes.forEach { note ->
                        noteViewModel.update(note.copy(folderId = it.id))
                    }
                    selectedFolders.forEach { folder ->
                        folderViewModel.moveFolderToParent(folder, it.id)
                    }
                    context.toast("Moved ${selectedNotes.size} note(s) and ${selectedFolders.size} folder(s) to '${it.name}'")
                    exitMultiSelectMode()
                }
            }
        }
    }


    fun handleCopy(
        selectedNotes: List<NoteEntity>,
        selectedFolders: List<FolderEntity>
    ) {
        if (selectedNotes.isEmpty() && selectedFolders.isEmpty()) return

        folderViewModel.allFolders.observeOnce(lifecycleOwner) { allFolders ->
            val excludedIds = selectedFolders.map { it.id }.toSet()

            showMultiFolderDialog(
                context = context,
                inflater = layoutInflater,
                folders = allFolders,
                excludedFolderIds = excludedIds,
                titleText = "Copy to",
                startFromFolder = null
            ) { targetFolder ->
                targetFolder?.let {
                    selectedNotes.forEach { note ->
                        noteViewModel.insert(note.copy(id = 0, folderId = it.id))
                    }
                    selectedFolders.forEach { folder ->
                        folderViewModel.copyFolderWithContents(folder, it.id) {
                            folderViewModel.refreshSubfolders(it.id, onlyIfVisible = 0)
                        }
                    }
                    context.toast("Copied ${selectedNotes.size} note(s) and ${selectedFolders.size} folder(s) to '${it.name}'")
                    exitMultiSelectMode()
                }
            }
        }
    }

    fun handleDelete(
        selectedNotes: List<NoteEntity>,
        selectedFolders: List<FolderEntity>
    ) {
        val totalItems = selectedNotes.size + selectedFolders.size
        if (totalItems == 0) return

        AlertDialog.Builder(context)
            .setTitle("Are you sure?")
            .setMessage("These $totalItems item(s) will be moved to Recently Deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                selectedNotes.forEach { note ->
                    noteViewModel.update(note.copy(isDeleted = true))
                }
                selectedFolders.forEach { folder ->
                    folderViewModel.deleteFolderAndNotes(folder)
                }
                context.toast("Deleted $totalItems item(s)")
                exitMultiSelectMode()
            }.show()
    }
}
