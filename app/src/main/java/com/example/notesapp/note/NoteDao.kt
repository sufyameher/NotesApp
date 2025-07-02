package com.example.notesapp.note

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(noteEntity: NoteEntity)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM note WHERE isDeleted = 1")
    suspend fun deleteAllDeletedNotes()

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity

    @Query("SELECT * FROM note WHERE id = :id")
    fun getNoteByIdLive(id: Int): LiveData<NoteEntity>

    @Query("SELECT * FROM note ORDER BY id DESC")
    fun getAllNotes(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0")
    fun getAllNotesToDelete(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 1")
    fun getDeletedNotes(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY modifiedDate DESC")
    fun getActiveNotes(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE folderId = :folderId AND isDeleted = 0")
    suspend fun getNotesByFolderIdRaw(folderId: Int): List<NoteEntity>

    @Query("SELECT * FROM note WHERE folderId = :folderId AND isDeleted = 0 ORDER BY modifiedDate DESC")
    fun getNotesByFolderId(folderId: Int): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY createdDate DESC")
    fun getNotesSortedByDateCreatedDesc(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY createdDate ASC")
    fun getNotesSortedByDateCreatedAsc(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY modifiedDate DESC")
    fun getNotesSortedByDateModifiedDesc(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY modifiedDate ASC")
    fun getNotesSortedByDateModifiedAsc(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY title COLLATE NOCASE ASC")
    fun getNotesSortedByTitleAsc(): LiveData<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE isDeleted = 0 ORDER BY title COLLATE NOCASE DESC")
    fun getNotesSortedByTitleDesc(): LiveData<List<NoteEntity>>

     @Query("""
        SELECT * FROM note 
        WHERE isDeleted = 0 AND folderId = :folderId 
        ORDER BY 
            CASE WHEN :sortBy = 'TITLE' AND :order = 'ASC' THEN title END COLLATE NOCASE ASC,
            CASE WHEN :sortBy = 'TITLE' AND :order = 'DESC' THEN title END COLLATE NOCASE DESC,
            CASE WHEN :sortBy = 'DATE_CREATED' AND :order = 'ASC' THEN createdDate END ASC,
            CASE WHEN :sortBy = 'DATE_CREATED' AND :order = 'DESC' THEN createdDate END DESC,
            CASE WHEN :sortBy = 'DATE_EDITED' AND :order = 'ASC' THEN modifiedDate END ASC,
            CASE WHEN :sortBy = 'DATE_EDITED' AND :order = 'DESC' THEN modifiedDate END DESC
    """)
    suspend fun getSortedNotesByFolderId(folderId: Int, sortBy: String, order: String): List<NoteEntity>

    @Query("""
        SELECT * FROM note 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
        ORDER BY modifiedDate DESC
    """)
    fun searchNotes(query: String): LiveData<List<NoteEntity>>
}
