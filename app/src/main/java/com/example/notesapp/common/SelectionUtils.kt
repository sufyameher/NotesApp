package com.example.notesapp.common

import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity

object SelectionUtils {

    fun toggleNoteSelection(
        note: NoteEntity,
        selectedNoteIds: MutableSet<Int>,
        noteAdapter: NoteAdapter,
        updateSelectionCount: () -> Unit
    ) {
        selectedNoteIds.toggleItem(note.id)
        noteAdapter.selectedIds = selectedNoteIds
        noteAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }

    fun toggleFolderSelection(
        folder: FolderEntity,
        selectedFolderIds: MutableSet<Int>,
        folderAdapter: FolderAdapter,
        updateSelectionCount: () -> Unit
    ) {
        selectedFolderIds.toggleItem(folder.id)
        folderAdapter.selectedIds = selectedFolderIds
        folderAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }

    fun handleSelectAll(
        latestNotes: List<NoteEntity>,
        latestSubfolders: List<FolderEntity>,
        selectedNoteIds: MutableSet<Int>,
        selectedFolderIds: MutableSet<Int>,
        noteAdapter: NoteAdapter,
        folderAdapter: FolderAdapter,
        updateSelectionCount: () -> Unit
    ) {
        selectedNoteIds.clear()
        selectedNoteIds.addAll(latestNotes.map { it.id })
        noteAdapter.selectedIds = selectedNoteIds

        selectedFolderIds.clear()
        selectedFolderIds.addAll(latestSubfolders.map { it.id })
        folderAdapter.selectedIds = selectedFolderIds

        noteAdapter.notifyDataSetChanged()
        folderAdapter.notifyDataSetChanged()
        updateSelectionCount()
    }
}
