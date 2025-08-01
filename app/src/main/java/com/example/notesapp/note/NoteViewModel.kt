package com.example.notesapp.note

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.example.notesapp.common.LiveDataHost
import com.example.notesapp.common.LiveDataHostNullable
import com.example.notesapp.common.PreferenceUtil
import com.example.notesapp.common.ViewMode
import com.example.notesapp.db.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = NoteRepository(AppDatabase.getInstance(application).noteDao())

    val viewMode: MutableLiveData<ViewMode> = MutableLiveData(PreferenceUtil.viewMode)

    val folderSortedNotes = LiveDataHost(viewModelScope, emptyList<NoteEntity>())

    val noteById = LiveDataHostNullable<NoteEntity>(viewModelScope)
    val searchResults = LiveDataHost(viewModelScope, emptyList<NoteEntity>())
    val folderNotes = LiveDataHost(viewModelScope, emptyList<NoteEntity>())

    val allNotesSorted = LiveDataHost(viewModelScope, emptyList<NoteEntity>())
    private val _activeNotes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val activeNotesFlow: StateFlow<List<NoteEntity>> = _activeNotes

    private var currentSortBy: String = "DATE_CREATED"
    private var currentSortOrder: String = "DESC"
    private var currentFolderId: Int = -1

    val rootFolderId = 0

    init {
        allNotesSorted(noteRepository.getAllNotesSortedFlow())
        sortNotes(rootFolderId, currentSortBy, currentSortOrder)
    }

    fun sortNotes(folderId: Int, sortBy: String, order: String) {
        currentSortBy = sortBy
        currentSortOrder = order
        currentFolderId = folderId

        viewModelScope.launch {
            noteRepository.getSortedNotesFlow(folderId, sortBy, order)
                .collect { sortedNotes ->
                    _activeNotes.value = sortedNotes
                }
        }
    }

    fun sortNotesInFolder(folderId: Int, sortBy: String, order: String) {
        currentSortBy = sortBy
        currentSortOrder = order
        currentFolderId = folderId

        viewModelScope.launch {
            val sorted = noteRepository.getSortedNotesByFolderIdFlow(folderId, sortBy, order)
            folderSortedNotes(sorted)
        }
    }

    fun togglePin(note: NoteEntity) = viewModelScope.launch {
        val pinnedNote = note.copy(
            isPinned = !note.isPinned,
        )
        Timber.d("Toggling pin: ${note.title} ${pinnedNote.isPinned}")

        noteRepository.update(pinnedNote)
        if (currentFolderId != -1) {
            sortNotesInFolder(currentFolderId, currentSortBy, currentSortOrder)
        }
    }

    fun setViewMode(mode: ViewMode) {
        viewMode.value = mode
        PreferenceUtil.viewMode = mode
    }

    fun insert(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insert(note)
        sortNotes(note.folderId, "DATE_CREATED", "DESC")
    }

    fun update(note: NoteEntity) = viewModelScope.launch {
        val oldNote = noteRepository.getNoteById(note.id)
        val isEdited = oldNote.title != note.title || oldNote.description != note.description
        val updated = note.copy(
            modifiedDate = System.currentTimeMillis(),
            isEdited = isEdited
        )
        noteRepository.update(updated)
        refreshFolderNotesOnUpdate(oldNote.folderId, note.folderId)
    }

    fun delete(note: NoteEntity) = viewModelScope.launch {
        noteRepository.delete(note)
        sortNotes(note.folderId, "DATE_CREATED", "DESC")
    }

    fun getNoteById(id: Int) {
        noteById(noteRepository.getNoteByIdLiveFlow(id))
    }

    fun searchNotes(query: String) {
        searchResults(noteRepository.searchNotesFlow(query))
    }

    fun getNotesByFolderId(folderId: Int) {
        folderNotes(noteRepository.getNotesByFolderIdFlow(folderId))
    }

    fun saveOrUpdateNote(
        title: String,
        desc: String,
        folderId: Int,
        existing: NoteEntity?
    ) {
        val now = System.currentTimeMillis()
        val finalTitle = if (desc.isNotEmpty()) "$title." else title

        val note = existing?.copy(
            title = finalTitle,
            description = desc,
            modifiedDate = now
        ) ?: NoteEntity(
            title = title,
            description = desc,
            createdDate = now,
            modifiedDate = now,
            folderId = folderId
        )

        if (existing != null) update(note) else insert(note)
    }

    private fun refreshFolderNotesOnUpdate(oldFolderId: Int, newFolderId: Int) {
        if (oldFolderId != newFolderId) {
            sortNotes(oldFolderId, "DATE_MODIFIED", "DESC")
        }
        sortNotes(newFolderId, "DATE_MODIFIED", "DESC")
    }
}
