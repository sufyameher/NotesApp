package com.example.notesapp.common.multiselect

class MultiSelection {
    val selectedNoteIds = mutableSetOf<Int>()
    val selectedFolderIds = mutableSetOf<Int>()
    var isMultiSelectEnabled = false

    fun clearSelections() {
        selectedNoteIds.clear()
        selectedFolderIds.clear()
        isMultiSelectEnabled = false
    }

    fun toggleNote(id: Int) {
        if (selectedNoteIds.contains(id)) selectedNoteIds.remove(id)
        else selectedNoteIds.add(id)
        isMultiSelectEnabled = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()
    }

    fun toggleFolder(id: Int) {
        if (selectedFolderIds.contains(id)) selectedFolderIds.remove(id)
        else selectedFolderIds.add(id)
        isMultiSelectEnabled = selectedNoteIds.isNotEmpty() || selectedFolderIds.isNotEmpty()
    }

    fun getSelectionCount() = selectedNoteIds.size + selectedFolderIds.size
}
