package com.example.notesapp.common.multiselect

import com.example.notesapp.common.gone
import com.example.notesapp.common.show
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity

object MultiSelectHelper {

    fun enableMultiSelect(
        binding: FolderActivityBinding,
        noteAdapter: NoteAdapter,
        folderAdapter: FolderAdapter,
        selectedNoteIds: MutableSet<Int>,
        selectedFolderIds: MutableSet<Int>,
        updateSelectionCount: () -> Unit
    ) {
        noteAdapter.isMultiSelectMode = true
        folderAdapter.isMultiSelectMode = true
        noteAdapter.selectedIds = selectedNoteIds
        folderAdapter.selectedIds = selectedFolderIds
        noteAdapter.notifyDataSetChanged()
        folderAdapter.notifyDataSetChanged()
        binding.topBar.root.gone()
        binding.selectionTopBar.show()
        updateSelectionCount()

    }

    fun exitMultiSelect(
        binding: FolderActivityBinding,
        noteAdapter: NoteAdapter,
        folderAdapter: FolderAdapter,
        selectedNoteIds: MutableSet<Int>,
        selectedFolderIds: MutableSet<Int>,
        updateSelectionCount: () -> Unit

    ) {
        selectedNoteIds.clear()
        selectedFolderIds.clear()
        noteAdapter.isMultiSelectMode = false
        folderAdapter.isMultiSelectMode = false
        noteAdapter.selectedIds = emptySet()
        folderAdapter.selectedIds = emptySet()
        noteAdapter.notifyDataSetChanged()
        folderAdapter.notifyDataSetChanged()
        binding.topBar.root.show()
        binding.selectionTopBar.gone()
        updateSelectionCount()

    }

    fun handleMove(
        latestNotes: List<NoteEntity>,
        latestSubfolders: List<FolderEntity>,
        selectedNoteIds: Set<Int>,
        selectedFolderIds: Set<Int>,
        multiSelectActionHandler: MultiSelectActionHandler
    ) {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleMove(selectedNotes, selectedFolders)
    }

    fun handleCopy(
        latestNotes: List<NoteEntity>,
        latestSubfolders: List<FolderEntity>,
        selectedNoteIds: Set<Int>,
        selectedFolderIds: Set<Int>,
        multiSelectActionHandler: MultiSelectActionHandler
    ) {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleCopy(selectedNotes, selectedFolders)
    }

    fun handleDelete(
        latestNotes: List<NoteEntity>,
        latestSubfolders: List<FolderEntity>,
        selectedNoteIds: Set<Int>,
        selectedFolderIds: Set<Int>,
        multiSelectActionHandler: MultiSelectActionHandler
    ) {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleDelete(selectedNotes, selectedFolders)
    }
}