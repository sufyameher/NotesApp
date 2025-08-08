package com.example.notesapp.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.LiveDataHost
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {

    val deletedNotes = LiveDataHost(viewModelScope, emptyList<NoteEntity>())

    fun loadDeletedNotes() {
        deletedNotes(noteRepository.getDeletedNotesFlow())
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
}