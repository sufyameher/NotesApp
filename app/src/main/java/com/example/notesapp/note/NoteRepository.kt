package com.example.notesapp.note

import com.example.notesapp.common.Helper.sortNotesList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {

    suspend fun insert(note: NoteEntity) = noteDao.insert(note)
    suspend fun update(note: NoteEntity) = noteDao.update(note)
    suspend fun delete(note: NoteEntity) = noteDao.delete(note)

    fun getAllNotes(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotes()
    }

    suspend fun insertDummyData(note: NoteEntity): Int {
        return noteDao.insertDummyData(note).toInt()
    }

    fun getSortedNotesFlow(folderId: Int, sortBy: String, order: String): Flow<List<NoteEntity>> {
        return noteDao.getNotesByFolderIdRawFlow(folderId)
            .map { notes -> sortNotesList(notes, sortBy, order) }
    }

    fun getRootNotesFlow(): Flow<List<NoteEntity>> {
        return noteDao.getRootNotesFlow()
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

     fun getNotesByFolderIdRaw(folderId: Int): Flow<List<NoteEntity>> {
        return noteDao.getNotesByFolderIdRawFlow(folderId)
    }

    fun getDeletedNotesFlow(): Flow<List<NoteEntity>> = noteDao.getDeletedNotesFlow()

    suspend fun permanentlyDeleteDeletedNotes(note: NoteEntity) {
        noteDao.delete(note)
    }

    suspend fun deleteAllNotes() {
        noteDao.deleteAllNotes()
    }

    suspend fun permanentlyDeleteAllDeletedNotes() {
        noteDao.deleteAllDeletedNotes()
    }

    fun getAllNotesSortedFlow(): Flow<List<NoteEntity>> {
        return noteDao.getAllNotesSortedFlow()
    }

    suspend fun getAllNotesOnce(): List<NoteEntity> {
        return noteDao.getAllNotesOnce()
    }
}
