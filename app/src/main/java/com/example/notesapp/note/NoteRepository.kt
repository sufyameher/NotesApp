package com.example.notesapp.note

import androidx.lifecycle.LiveData

class NoteRepository(private val noteDao: NoteDao) {

    fun getSortedNotes(sortBy: String, order: String): LiveData<List<NoteEntity>> {
        return when ("$sortBy-$order") {
            "DATE_CREATED-ASC"  -> noteDao.getNotesSortedByDateCreatedAsc()
            "DATE_CREATED-DESC" -> noteDao.getNotesSortedByDateCreatedDesc()
            "DATE_MODIFIED-ASC" -> noteDao.getNotesSortedByDateModifiedAsc()
            "DATE_MODIFIED-DESC"-> noteDao.getNotesSortedByDateModifiedDesc()
            "TITLE-ASC"         -> noteDao.getNotesSortedByTitleAsc()
            "TITLE-DESC"        -> noteDao.getNotesSortedByTitleDesc()
            else                -> noteDao.getNotesSortedByDateCreatedDesc()
        }
    }

    suspend fun insert(note: NoteEntity) = noteDao.insert(note)
    suspend fun update(note: NoteEntity) = noteDao.update(note)

    fun getNoteByIdLive(id: Int): LiveData<NoteEntity> {
        return noteDao.getNoteByIdLive(id)
    }

    suspend fun getNoteById(id: Int): NoteEntity {
        return noteDao.getNoteById(id)
    }

    fun searchNotes(query: String): LiveData<List<NoteEntity>> {
        return noteDao.searchNotes(query)
    }

    fun getNotesByFolderId(folderId: Int): LiveData<List<NoteEntity>> {
        return noteDao.getNotesByFolderId(folderId)
    }

    suspend fun getNotesByFolderIdRaw(folderId: Int): List<NoteEntity> {
        return noteDao.getNotesByFolderIdRaw(folderId)
    }

    suspend fun delete(note: NoteEntity) {
        noteDao.delete(note)
    }

    fun getDeletedNotes(): LiveData<List<NoteEntity>> = noteDao.getDeletedNotes()

    val allNotes: LiveData<List<NoteEntity>> = noteDao.getActiveNotes()

    suspend fun permanentlyDeleteDeletedNotes(note: NoteEntity) {
        noteDao.delete(note)
    }

    suspend fun permanentlyDeleteAllDeletedNotes() {
        noteDao.deleteAllDeletedNotes()
    }

    suspend fun getSortedNotesByFolderId(folderId: Int, sortBy: String, order: String): List<NoteEntity> {
        return noteDao.getSortedNotesByFolderId(folderId, sortBy, order)
    }




}
