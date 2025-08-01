package com.example.notesapp.note

import com.example.notesapp.common.Helper.sortNotesList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class NoteRepository(private val noteDao: NoteDao) {

    suspend fun insert(note: NoteEntity) = noteDao.insert(note)
    suspend fun update(note: NoteEntity) = noteDao.update(note)
    suspend fun delete(note: NoteEntity) = noteDao.delete(note)

    fun getSortedNotesFlow(folderId: Int, sortBy: String, order: String): Flow<List<NoteEntity>> = flow {
        emit(sortNotesList(noteDao.getNotesByFolderIdRaw(folderId), sortBy, order))
    }

    fun getSortedNotesByFolderIdFlow(folderId: Int, sortBy: String, order: String): Flow<List<NoteEntity>> = flow {
        emit(sortNotesList(noteDao.getNotesByFolderIdRaw(folderId), sortBy, order))
    }

    fun getNoteByIdLiveFlow(id: Int): Flow<NoteEntity> {
        return noteDao.getNoteByIdLiveFlow(id)
    }

    suspend fun getNoteById(id: Int): NoteEntity {
        return noteDao.getNoteById(id)
    }

    fun searchNotesFlow(query: String): Flow<List<NoteEntity>> {
        return noteDao.searchNotesFlow(query)
    }

    fun getNotesByFolderIdFlow(folderId: Int): Flow<List<NoteEntity>> {
        return noteDao.getNotesByFolderIdFlow(folderId)
    }

    suspend fun getNotesByFolderIdRaw(folderId: Int): List<NoteEntity> {
        return noteDao.getNotesByFolderIdRaw(folderId)
    }

    fun getDeletedNotesFlow(): Flow<List<NoteEntity>> = noteDao.getDeletedNotesFlow()

    suspend fun permanentlyDeleteDeletedNotes(note: NoteEntity) {
        noteDao.delete(note)
    }

    suspend fun permanentlyDeleteAllDeletedNotes() {
        noteDao.deleteAllDeletedNotes()
    }

    fun getAllNotesSortedFlow(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotesSortedFlow()
    }
}
