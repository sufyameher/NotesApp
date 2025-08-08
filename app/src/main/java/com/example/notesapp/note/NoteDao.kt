package com.example.notesapp.note

 import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun insert(noteEntity: NoteEntity)

    @Insert
    suspend fun insertDummyData(noteEntity: NoteEntity): Long

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("DELETE FROM note")
    suspend fun deleteAllNotes()

    @Query("SELECT * FROM note WHERE isDeleted = 0")
    suspend fun getAllNotesOnce(): List<NoteEntity>

    @Query("DELETE FROM note WHERE isDeleted = 1")
    suspend fun deleteAllDeletedNotes()

    @Query("SELECT * FROM note WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity

    @Query("SELECT * FROM note WHERE id = :id")
    fun getNoteByIdLiveFlow(id: Int): Flow<NoteEntity>

    @Query("SELECT * FROM note WHERE isDeleted = 1")
    fun getDeletedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE  folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, createdDate DESC")
    fun getActiveNotesFlow(folderId: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE folderId = :folderId AND isDeleted = 0")
    fun getNotesByFolderIdRawFlow(folderId: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE folderId = :folderId AND isDeleted = 0 ORDER BY isPinned DESC, createdDate DESC") // inner folder
    fun getNotesByFolderIdFlow(folderId: Int): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note ORDER BY isPinned DESC, createdDate DESC")
    fun getAllNotesSortedFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM note WHERE folderId = 0 AND isDeleted = 0 ORDER BY isPinned DESC, modifiedDate DESC")
    fun getRootNotesFlow(): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM note 
        WHERE isDeleted = 0 AND folderId = :folderId 
        ORDER BY 
            isPinned DESC,
            CASE WHEN :sortBy = 'TITLE' AND :order = 'A - Z' THEN title END COLLATE NOCASE ASC,
            CASE WHEN :sortBy = 'TITLE' AND :order = 'Z - A' THEN title END COLLATE NOCASE DESC,
            CASE WHEN :sortBy = 'DATE_CREATED' AND :order = 'ASCENDING' THEN createdDate END ASC,
            CASE WHEN :sortBy = 'DATE_CREATED' AND :order = 'DESCENDING' THEN createdDate END DESC,
            CASE WHEN :sortBy = 'DATE_EDITED' AND :order = 'ASCENDING' THEN modifiedDate END ASC,
            CASE WHEN :sortBy = 'DATE_EDITED' AND :order = 'DESCENDING' THEN modifiedDate END DESC
    """
    )
    fun getSortedNotesByFolderId(folderId: Int, sortBy: String, order: String): Flow<List<NoteEntity>>

    @Query(
        """
        SELECT * FROM note 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
        ORDER BY modifiedDate DESC
    """
    )
    fun searchNotesFlow(query: String): Flow<List<NoteEntity>>
}