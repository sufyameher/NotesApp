package com.example.notesapp.util

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.R
import com.example.notesapp.folder.ui.FolderActionBottomSheet
import com.example.notesapp.common.Helper.hideKeyboard
import com.example.notesapp.common.Helper.showKeyboard
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.note.NoteActionBottomSheet
import com.example.notesapp.common.ViewMode
import com.example.notesapp.databinding.DialogNewFolderBinding
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.note.NoteEntity

fun showDeleteForeverConfirmation(context: Context, onConfirm: () -> Unit) {
    AlertDialog.Builder(context)
        .setTitle("Delete Forever?")
        .setMessage("This note and its content will be deleted permanently and cannot be recovered.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Delete") { _, _ -> onConfirm() }
        .show()
}

fun showMoveConfirmDialog(
    context: Context,
    inflater: LayoutInflater,
    folders: List<FolderEntity>,
    currentFolderId: Int,
    startFromFolder: FolderEntity? = null,
    titleText: String,
    onConfirm: (FolderEntity?) -> Unit
) {
    val dialogView = inflater.inflate(R.layout.dialog_move_to, null)
    val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
    tvTitle?.text = titleText


    val tvBreadcrumb = dialogView.findViewById<TextView>(R.id.tvBreadcrumb)
    val folderListContainer = dialogView.findViewById<RadioGroup>(R.id.rgFolders)
    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

    val dialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(true)
        .create()

    dialog.show()

    dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rounded_dialog)
    dialog.window?.setLayout(
        (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val pathStack = mutableListOf<FolderEntity?>()
    var selectedFolder: FolderEntity? = startFromFolder

    val idToFolder = folders.associateBy { it.id }
    val tempStack = mutableListOf<FolderEntity>()

    val startFrom = startFromFolder
    var current: FolderEntity? = startFrom?.let { idToFolder[it.parentFolderId] }

    while (current != null) {
        tempStack.add(current)
        current = idToFolder[current.parentFolderId]
    }

    pathStack.add(null) // Home
    pathStack.addAll(tempStack.reversed())

    startFrom?.let { pathStack.add(it) }

    var selectedView: View? = null

    fun getSubfolders(parentId: Int?): List<FolderEntity> {
        return folders.filter { it.parentFolderId == parentId }
    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun updateUI() {
        val pathNames = pathStack.map { it?.name ?: "Home" }
        tvBreadcrumb.text = pathNames.joinToString(" > ")

        folderListContainer.removeAllViews()
        val currentParent = pathStack.lastOrNull()
        val subfolders = getSubfolders(currentParent?.id)
            .filter { it.id != currentFolderId } // ✅ exclude the folder being moved

        subfolders.forEach { folder ->
            val itemView = inflater.inflate(R.layout.item_folder_radio, folderListContainer, false)
            val container = itemView.findViewById<LinearLayout>(R.id.itemContainer)
            val tvName = itemView.findViewById<TextView>(R.id.tvFolderName)

            tvName.text = folder.name

            val isSelected = folder.id == selectedFolder?.id
            container.setBackgroundResource(
                if (isSelected) R.drawable.bg_folder_item_selected
                else R.drawable.bg_folder_item_normal
            )
            if (isSelected) selectedView = container

            container.setOnClickListener {
                selectedFolder = folder
                selectedView?.setBackgroundResource(R.drawable.bg_folder_item_normal)
                container.setBackgroundResource(R.drawable.bg_folder_item_selected)
                selectedView = container

                val childFolders = getSubfolders(folder.id)
                    .filter { it.id != currentFolderId } // ✅ prevent going into the folder being moved
                if (childFolders.isNotEmpty()) {
                    pathStack.add(folder)
                    updateUI()
                }
            }

            folderListContainer.addView(itemView)
        }

        tvBreadcrumb.setOnClickListener {
            if (pathStack.size > 1) {
                pathStack.removeAt(pathStack.lastIndex)
                selectedFolder = pathStack.lastOrNull()
                updateUI()
            }
        }
    }

    updateUI()

    btnCancel.setOnClickListener { dialog.dismiss() }

    btnConfirm.setOnClickListener {
        if (selectedFolder == null) {
            Toast.makeText(context, "Please select a folder before confirming", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        if (selectedFolder?.id == currentFolderId) {
            Toast.makeText(context, "Cannot copy/move into the same folder", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        dialog.dismiss()
        onConfirm(selectedFolder)
    }

}

fun showMultiFolderDialog(
    context: Context,
    inflater: LayoutInflater,
    folders: List<FolderEntity>,
    excludedFolderIds: Set<Int>,
    startFromFolder: FolderEntity? = null,
    titleText: String,
    onConfirm: (FolderEntity?) -> Unit
) {
    val dialogView = inflater.inflate(R.layout.dialog_move_to, null)
    val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
    tvTitle?.text = titleText

    val tvBreadcrumb = dialogView.findViewById<TextView>(R.id.tvBreadcrumb)
    val folderListContainer = dialogView.findViewById<RadioGroup>(R.id.rgFolders)
    val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
    val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)

    val dialog = AlertDialog.Builder(context)
        .setView(dialogView)
        .setCancelable(true)
        .create()

    dialog.show()

    dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rounded_dialog)
    dialog.window?.setLayout(
        (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    val pathStack = mutableListOf<FolderEntity?>()
    var selectedFolder: FolderEntity? = startFromFolder

    val idToFolder = folders.associateBy { it.id }
    val tempStack = mutableListOf<FolderEntity>()

    val startFrom = startFromFolder
    var current: FolderEntity? = startFrom?.let { idToFolder[it.parentFolderId] }

    while (current != null) {
        tempStack.add(current)
        current = idToFolder[current.parentFolderId]
    }

    pathStack.add(null) // Home
    pathStack.addAll(tempStack.reversed())
    startFrom?.let { pathStack.add(it) }

    var selectedView: View? = null

    fun getSubfolders(parentId: Int?): List<FolderEntity> {
        return folders.filter { it.parentFolderId == parentId && it.id !in excludedFolderIds }
    }

    fun updateUI() {
        val pathNames = pathStack.map { it?.name ?: "Home" }
        tvBreadcrumb.text = pathNames.joinToString(" > ")

        folderListContainer.removeAllViews()
        val currentParent = pathStack.lastOrNull()
        val subfolders = getSubfolders(currentParent?.id)

        subfolders.forEach { folder ->
            val itemView = inflater.inflate(R.layout.item_folder_radio, folderListContainer, false)
            val container = itemView.findViewById<LinearLayout>(R.id.itemContainer)
            val tvName = itemView.findViewById<TextView>(R.id.tvFolderName)

            tvName.text = folder.name

            val isSelected = folder.id == selectedFolder?.id
            container.setBackgroundResource(
                if (isSelected) R.drawable.bg_folder_item_selected
                else R.drawable.bg_folder_item_normal
            )
            if (isSelected) selectedView = container

            container.setOnClickListener {
                selectedFolder = folder
                selectedView?.setBackgroundResource(R.drawable.bg_folder_item_normal)
                container.setBackgroundResource(R.drawable.bg_folder_item_selected)
                selectedView = container

                val childFolders = getSubfolders(folder.id)
                if (childFolders.isNotEmpty()) {
                    pathStack.add(folder)
                    updateUI()
                }
            }

            folderListContainer.addView(itemView)
        }

        tvBreadcrumb.setOnClickListener {
            if (pathStack.size > 1) {
                pathStack.removeAt(pathStack.lastIndex)
                selectedFolder = pathStack.lastOrNull()
                updateUI()
            }
        }
    }

    updateUI()

    btnCancel.setOnClickListener { dialog.dismiss() }

    btnConfirm.setOnClickListener {
        if (selectedFolder == null) {
            Toast.makeText(context, "Please select a folder before confirming", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        if (selectedFolder?.id in excludedFolderIds) {
            Toast.makeText(context, "Cannot move folders into themselves", Toast.LENGTH_SHORT).show()
            return@setOnClickListener
        }

        dialog.dismiss()
        onConfirm(selectedFolder)
    }
}





fun getLayoutManager(context: Context, mode: ViewMode): RecyclerView.LayoutManager {
    return when (mode) {
        ViewMode.LIST -> LinearLayoutManager(context)
        ViewMode.GRID -> GridLayoutManager(context, 2)
    }
}

fun showNewFolderDialog(
    context: Context,
    parentFolderId: Int? = null,
    onFolderCreated: (FolderEntity) -> Unit
) {
    val binding = DialogNewFolderBinding.inflate(LayoutInflater.from(context))

    val dialog = AlertDialog.Builder(context)
        .setView(binding.root)
        .setCancelable(true)
        .create()

    dialog.setOnShowListener {
        binding.etFolderName.requestFocus()
        binding.etFolderName.postDelayed({
            binding.etFolderName.showKeyboard()
        }, 200)
    }

    binding.btnCancel.setOnClickListener {
        binding.etFolderName.hideKeyboard()
        dialog.dismiss()
    }

    binding.btnConfirm.setOnClickListener {
        val folderName = binding.etFolderName.text.toString().trim()

        if (folderName.isEmpty()) {
            binding.etFolderName.error = "Folder name cannot be empty"
            return@setOnClickListener
        }

        val newFolder = FolderEntity(name = folderName, parentFolderId = parentFolderId)
        binding.etFolderName.hideKeyboard()
        dialog.dismiss()
        onFolderCreated(newFolder)
    }

    dialog.show()
}


fun showNoteActions(
    context: Context,
    fragmentManager: FragmentManager,
    note: NoteEntity,
    getFolders: (onResult: (List<FolderEntity>) -> Unit) -> Unit,
    onNoteUpdated: (NoteEntity) -> Unit,
    onNoteInserted: (NoteEntity) -> Unit
) {
    NoteActionBottomSheet(
        note = note,

        onMoveTo = { selectedNote ->
            getFolders { folders ->
                val currentFolderId = selectedNote.folderId
                val availableFolders = folders.filter { it.id != currentFolderId }
                val idToFolderMap = folders.associateBy { it.id }
                val currentFolder = idToFolderMap[currentFolderId]



                if (availableFolders.isEmpty()) {
                    Toast.makeText(context, "No folders available to move", Toast.LENGTH_SHORT).show()
                    return@getFolders
                }

                showMoveConfirmDialog(
                    context = context,
                    inflater = LayoutInflater.from(context),
                    folders = folders, // ✅ full list (not just availableFolders)
                    currentFolderId = currentFolderId ?: -1,
                    titleText = "Move to",
                    startFromFolder = currentFolder // ✅ this makes breadcrumb correct
                ) { targetFolder ->
                    val updatedNote = selectedNote.copy(folderId = targetFolder?.id)
                    onNoteUpdated(updatedNote)
                    context.toast("Note moved to '${targetFolder?.name}'")
                }
            }
        },

        onCopyTo = { selectedNote ->
            getFolders { folders ->
                val currentFolderId = selectedNote.folderId
                val availableFolders = folders.filter { it.id != currentFolderId }

                if (availableFolders.isEmpty()) {
                    Toast.makeText(context, "No folders available to copy", Toast.LENGTH_SHORT).show()
                    return@getFolders
                }

                showMoveConfirmDialog(
                    context = context,
                    inflater = LayoutInflater.from(context),
                    folders = availableFolders,
                    currentFolderId = currentFolderId ?: -1,
                    titleText = "Copy to",

                    ) { targetFolder ->
                    targetFolder?.let {
                        val copiedNote = selectedNote.copy(
                            id = 0,
                            folderId = it.id
                        )
                        onNoteInserted(copiedNote)
                        context.toast("Note copied to '${it.name}'")
                    }
                }
            }
        },

        onDelete = { selectedNote ->
            AlertDialog.Builder(context)
                .setTitle("Are you sure?")
                .setMessage("Deleted notes and their content will be moved to Recently Deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    val deletedNote = selectedNote.copy(isDeleted = true)
                    onNoteUpdated(deletedNote)
                    Toast.makeText(context, "Note deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .show()
        }

    ).show(fragmentManager, "NoteActionBottomSheet")
}

fun handleMoveNotes(
    context: Context,
    inflater: LayoutInflater,
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    onUpdate: (NoteEntity) -> Unit
) {
    if (notes.isEmpty()) return

    val currentFolderId = notes.first().folderId
    val currentFolder = folders.find { it.id == currentFolderId }

    if (folders.isEmpty()) {
        Toast.makeText(context, "No folders available to move", Toast.LENGTH_SHORT).show()
        return
    }

    showMoveConfirmDialog(
        context = context,
        inflater = inflater,
        folders = folders,
        currentFolderId = currentFolderId ?: -1,
        startFromFolder = currentFolder,
        titleText = "Move to"
    ) { targetFolder ->
        targetFolder?.let {
            notes.forEach { note ->
                val updatedNote = note.copy(folderId = it.id)
                onUpdate(updatedNote)
            }
            context.toast("Moved to '${it.name}'")
        }
    }
}

fun handleCopyNotes(
    context: Context,
    inflater: LayoutInflater,
    notes: List<NoteEntity>,
    folders: List<FolderEntity>,
    onInsert: (NoteEntity) -> Unit
) {
    if (notes.isEmpty()) return

    val currentFolderId = notes.first().folderId
    val availableFolders = folders.filter { it.id != currentFolderId }

    if (availableFolders.isEmpty()) {
        Toast.makeText(context, "No folders available to copy", Toast.LENGTH_SHORT).show()
        return
    }

    showMoveConfirmDialog(
        context = context,
        inflater = inflater,
        folders = availableFolders,
        currentFolderId = currentFolderId ?: -1,
        titleText = "Copy to"
    ) { targetFolder ->
        targetFolder?.let {
            notes.forEach { note ->
                val copiedNote = note.copy(id = 0, folderId = it.id)
                onInsert(copiedNote)
            }
            context.toast("Copied to '${it.name}'")
        }
    }
}

fun confirmDeleteNotes(
    context: Context,
    notes: List<NoteEntity>,
    onUpdate: (NoteEntity) -> Unit
) {
    if (notes.isEmpty()) return

    AlertDialog.Builder(context)
        .setTitle("Are you sure?")
        .setMessage("Selected notes will be moved to Recently Deleted.")
        .setNegativeButton("Cancel", null)
        .setPositiveButton("Delete") { _, _ ->
            notes.forEach { note ->
                onUpdate(note.copy(isDeleted = true))
            }
            context.toast("Notes moved to Recently Deleted")
        }
        .show()
}

fun showFolderActions(
    context: Context,
    fragmentManager: FragmentManager,
    folder: FolderEntity,
    getAvailableFolders: (onResult: (List<FolderEntity>) -> Unit) -> Unit,
    onMoveTo: (FolderEntity, FolderEntity) -> Unit,
    onCopyTo: (fromFolderId: Int, toFolderId: Int) -> Unit,
    onDelete: (FolderEntity) -> Unit
) {
    FolderActionBottomSheet(
        folder = folder,
        folderInfo = null,

        onMoveTo = { selectedFolder ->
            getAvailableFolders { availableFolders ->
                val filtered = availableFolders.filter { it.id != selectedFolder.id }

                if (filtered.isEmpty()) {
                    Toast.makeText(context, "No folders available to move", Toast.LENGTH_SHORT).show()
                    return@getAvailableFolders
                }

                showMoveConfirmDialog(
                    context = context,
                    inflater = LayoutInflater.from(context),
                    folders = filtered,
                    currentFolderId = selectedFolder.id,
                    titleText = "Move to",

                    ) { targetFolder ->
                    targetFolder?.let {
                        onMoveTo(selectedFolder, it)
                        Toast.makeText(context, "Folder moved to '${it.name}'", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },

        onCopyTo = { selectedFolder ->
            getAvailableFolders { availableFolders ->
                val filtered = availableFolders.filter { it.id != selectedFolder.id }

                if (filtered.isEmpty()) {
                    Toast.makeText(context, "No folders available to copy to", Toast.LENGTH_SHORT).show()
                    return@getAvailableFolders
                }

                showMoveConfirmDialog(
                    context = context,
                    inflater = LayoutInflater.from(context),
                    folders = filtered,
                    currentFolderId = selectedFolder.id,
                    titleText = "Copy to",

                    ) { targetFolder ->
                    targetFolder?.let {
                        onCopyTo(selectedFolder.id, it.id)
                        Toast.makeText(context, "Notes copied to '${it.name}'", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },

        onDelete = { selectedFolder ->
            AlertDialog.Builder(context)
                .setTitle("Are you sure?")
                .setMessage("This folder and its notes will be moved to Recently Deleted.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ ->
                    onDelete(selectedFolder)
                    Toast.makeText(context, "Folder deleted successfully", Toast.LENGTH_SHORT).show()
                }
                .show()
        }
    ).show(fragmentManager, "FolderActionBottomSheet")
}


