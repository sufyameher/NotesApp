package com.example.notesapp.trash

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.LiveDataHost
import com.example.notesapp.db.AppDatabase
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteRepository
import kotlinx.coroutines.launch

class TrashViewModel(application: Application) : AndroidViewModel(application) {
    private val noteRepository = NoteRepository(AppDatabase.getInstance(application).noteDao())

    val deletedNotes = LiveDataHost(viewModelScope, emptyList<NoteEntity>())

    init {
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