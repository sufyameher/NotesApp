package com.example.notesapp.note

import android.app.Application
import androidx.lifecycle.*
import com.example.notesapp.common.ViewMode
import com.example.notesapp.db.AppDatabase
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = NoteRepository(AppDatabase.getInstance(application).noteDao())

    val viewMode: MutableLiveData<ViewMode> = MutableLiveData(ViewMode.LIST)
    val allNotes: MutableLiveData<List<NoteEntity>> = MutableLiveData()
    val folderSortedNotes: MutableLiveData<List<NoteEntity>> = MutableLiveData()

    val deletedNotes: LiveData<List<NoteEntity>> = noteRepository.getDeletedNotes()
    val activeNotes: LiveData<List<NoteEntity>> = noteRepository.allNotes

    init {
        sortNotes("DATE_CREATED", "DESC")
    }

    fun setViewMode(mode: ViewMode) {
        viewMode.value = mode
    }

    fun sortNotes(sortBy: String, order: String) {
        noteRepository.getSortedNotes(sortBy, order).observeForever { sorted ->
            allNotes.value = sorted
        }
    }

    fun sortNotesInFolder(folderId: Int, sortBy: String, order: String) {
        viewModelScope.launch {
            val sorted = noteRepository.getSortedNotesByFolderId(folderId, sortBy, order)
            folderSortedNotes.postValue(sorted)
        }
    }

    fun insert(note: NoteEntity) = viewModelScope.launch {
        noteRepository.insert(note)
        sortNotes("DATE_CREATED", "DESC")
    }

    fun update(note: NoteEntity) = viewModelScope.launch {
        val oldNote = noteRepository.getNoteById(note.id)
        val isEdited = oldNote.title != note.title || oldNote.description != note.description
        val updated = note.copy(
            modifiedDate = System.currentTimeMillis(),
            isEdited = isEdited
        )
        noteRepository.update(updated)
        sortNotes("DATE_MODIFIED", "DESC")
    }

    fun delete(note: NoteEntity) = viewModelScope.launch {
        noteRepository.delete(note)
        sortNotes("DATE_CREATED", "DESC")
    }

    fun permanentlyDelete(note: NoteEntity) = viewModelScope.launch {
        noteRepository.permanentlyDeleteDeletedNotes(note)
    }

    fun permanentlyDeleteAll() = viewModelScope.launch {
        noteRepository.permanentlyDeleteAllDeletedNotes()
    }

    fun recover(note: NoteEntity) = viewModelScope.launch {
        noteRepository.update(note.copy(isDeleted = false))
    }

    fun getNoteById(id: Int): LiveData<NoteEntity> {
        return noteRepository.getNoteByIdLive(id)
    }

    fun searchNotes(query: String): LiveData<List<NoteEntity>> {
        return noteRepository.searchNotes(query)
    }

    fun getNotesByFolderId(folderId: Int): LiveData<List<NoteEntity>> {
        return noteRepository.getNotesByFolderId(folderId)
    }

    fun moveNoteToFolder(fromFolderId: Int, toFolderId: Int) = viewModelScope.launch {
        val notes = noteRepository.getNotesByFolderIdRaw(fromFolderId)
        notes.forEach {
            noteRepository.update(it.copy(folderId = toFolderId))
        }
    }

    fun copyAllNotesFromFolderTo(fromFolderId: Int, toFolderId: Int) = viewModelScope.launch {
        val notes = noteRepository.getNotesByFolderIdRaw(fromFolderId)
        notes.forEach {
            noteRepository.insert(it.copy(id = 0, folderId = toFolderId))
        }
    }

    fun saveOrUpdateNote(
        title: String,
        desc: String,
        folderId: Int?,
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

}
