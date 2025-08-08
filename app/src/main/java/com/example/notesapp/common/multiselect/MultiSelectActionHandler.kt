 package com.example.notesapp.common.multiselect

import android.app.AlertDialog
import android.content.Context
import android.view.LayoutInflater
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LifecycleOwner
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.MainActivityViewModel
import com.example.notesapp.util.showMultiFolderDialog

class MultiSelectActionHandler(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val mainActivityViewModel: MainActivityViewModel,
    private val folderActivityViewModel: FolderActivityViewModel,
    private val fragmentManager: FragmentManager,
    private val layoutInflater: LayoutInflater,
    private val exitMultiSelectMode: () -> Unit,
    private val refreshRootFoldersUI: (List<FolderEntity>) -> Unit,
    private val filterVisibleFolders: (List<FolderEntity>) -> List<FolderEntity>


) {
    fun handleMove(
        selectedNotes: List<NoteEntity>,
        selectedFolders: List<FolderEntity>
    ) {
        if (selectedNotes.isEmpty() && selectedFolders.isEmpty()) return

        folderActivityViewModel.activeFolders.observeOnce(lifecycleOwner) { allFolders ->
            val excludedIds = selectedFolders.map { it.id }.toSet()

            val startFromFolder = when {
                selectedFolders.isNotEmpty() -> allFolders.find { it.id == selectedFolders.first().id }
                selectedNotes.isNotEmpty() -> allFolders.find { it.id == selectedNotes.first().folderId }
                else -> null
            }

            showMultiFolderDialog(
                context = context,
                inflater = layoutInflater,
                folders = allFolders,
                excludedFolderIds = excludedIds,
                titleText = "Move to",
                startFromFolder = startFromFolder
            ) { targetFolder ->
                targetFolder?.let {
                    selectedNotes.forEach { note ->
                        mainActivityViewModel.update(note.copy(folderId = it.id))
                    }
                    selectedFolders.forEach { folder ->
                        folderActivityViewModel.moveFolderToParent(folder, it.id)
                    }

                    val updatedVisibleFolders = filterVisibleFolders(allFolders)
                        .filterNot { selectedFolders.map { it.id }.toSet().contains(it.id) }

                    refreshRootFoldersUI(updatedVisibleFolders)

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

        folderActivityViewModel.allFolders.observeOnce(lifecycleOwner) { allFolders ->
            val excludedIds = selectedFolders.map { it.id }.toSet()

            val startFromFolder = when {
                selectedFolders.isNotEmpty() -> allFolders.find { it.id == selectedFolders.first().id }
                selectedNotes.isNotEmpty() -> allFolders.find { it.id == selectedNotes.first().folderId }
                else -> null
            }


            showMultiFolderDialog(
                context = context,
                inflater = layoutInflater,
                folders = allFolders,
                excludedFolderIds = excludedIds,
                titleText = "Copy to",
                startFromFolder = startFromFolder
            ) { targetFolder ->
                targetFolder?.let {
                    selectedNotes.forEach { note ->
                        mainActivityViewModel.insert(note.copy(id = 0, folderId = it.id))
                    }
                    selectedFolders.forEach { folder ->
                        folderActivityViewModel.copyFolderWithContents(folder, it.id) {
                            folderActivityViewModel.refreshSubfolders(it.id, onlyIfVisible = 0)
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
                    mainActivityViewModel.update(note.copy(isDeleted = true))
                }
                selectedFolders.forEach { folder ->
                    folderActivityViewModel.deleteFolderAndNotes(folder)
                }
                context.toast("Deleted $totalItems item(s)")
                exitMultiSelectMode()
            }.show()
    }
}
