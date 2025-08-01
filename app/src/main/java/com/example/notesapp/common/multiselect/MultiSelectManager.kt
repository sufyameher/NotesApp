package com.example.notesapp.common.multiselect

class MultiSelectManager<T>(
    private val onSelectionChanged: (Set<T>) -> Unit,
    private val onMultiSelectModeChanged: (Boolean) -> Unit
) {
    private val selectedItems = mutableSetOf<T>()
    var isMultiSelectMode = false
        private set

    fun toggleSelection(item: T) {
        if (selectedItems.contains(item)) {
            selectedItems.remove(item)
        } else {
            selectedItems.add(item)
        }
        updateMode()
    }

    fun clearSelection() {
        selectedItems.clear()
        updateMode()
    }

    fun selectAll(items: List<T>) {
        selectedItems.clear()
        selectedItems.addAll(items)
        updateMode()
    }

    fun getSelected(): Set<T> = selectedItems.toSet()

    private fun updateMode() {
        val active = selectedItems.isNotEmpty()
        if (active != isMultiSelectMode) {
            isMultiSelectMode = active
            onMultiSelectModeChanged(active)
        }
        onSelectionChanged(selectedItems)
    }
}
