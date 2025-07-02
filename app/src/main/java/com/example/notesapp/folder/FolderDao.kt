package com.example.notesapp.folder

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderToCopy(folder: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Int): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY createdDate DESC")
    fun getAllFolders(): LiveData<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY createdDate DESC")
    fun getActiveFolders(): LiveData<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY createdDate DESC")
    fun getDeletedFolders(): LiveData<List<FolderEntity>>

    @Query("""
        SELECT * FROM folders 
        WHERE parentFolderId = :parentId AND isDeleted = 0 
        ORDER BY createdDate DESC
    """)
    fun getSubfolders(parentId: Int): LiveData<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId")
    suspend fun getSubfoldersRaw(parentId: Int): List<FolderEntity>


    @Query("""
        SELECT * FROM folders 
        WHERE isDeleted = 0 AND name LIKE :query 
        ORDER BY name
    """)
    fun searchFolders(query: String): LiveData<List<FolderEntity>>

    @Query("""
        SELECT f.*, 
               (SELECT COUNT(*) FROM folders sf WHERE sf.parentFolderId = f.id AND sf.isDeleted = 0) AS subfolderCount,
               (SELECT COUNT(*) FROM note n WHERE n.folderId = f.id AND n.isDeleted = 0) AS noteCount
        FROM folders f
        WHERE f.isDeleted = 0 AND f.name LIKE :query
    """)
    fun searchFolderSummaries(query: String): LiveData<List<FolderWithNoteCount>>

    @Transaction
    @Query("""
        SELECT f.*, 
               (SELECT COUNT(*) FROM folders AS sub WHERE sub.parentFolderId = f.id AND sub.isDeleted = 0) AS subfolderCount,
               (SELECT COUNT(*) FROM note AS n WHERE n.folderId = f.id AND n.isDeleted = 0) AS noteCount
        FROM folders AS f
        WHERE f.id = :folderId
        LIMIT 1
    """)
    fun getFolderSummary(folderId: Int): LiveData<FolderWithNoteCount>
}
