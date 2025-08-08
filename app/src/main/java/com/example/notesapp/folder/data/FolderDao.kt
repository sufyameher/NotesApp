package com.example.notesapp.folder.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.model.FolderWithNoteCount
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolderToCopy(folder: FolderEntity): Long

    @Insert
    suspend fun insert(folderEntity: FolderEntity): Long

    @Update
    suspend fun updateFolder(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    fun getFolderLiveFlow(folderId: Int): Flow<FolderEntity?>

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Delete
    suspend fun delete(folder: FolderEntity)

    @Query("SELECT * FROM folders WHERE id = :folderId LIMIT 1")
    suspend fun getFolderById(folderId: Int): FolderEntity?

    @Query("SELECT * FROM folders ORDER BY createdDate DESC")
    fun getAllFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 0 ORDER BY createdDate DESC")
    fun getActiveFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isDeleted = 1 ORDER BY createdDate DESC")
    fun getDeletedFoldersFlow(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId AND isDeleted = 0 ORDER BY createdDate DESC")
    fun getSubfoldersFlow(parentId: Int): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE parentFolderId = :parentId AND isDeleted = 0")
    suspend fun getSubfoldersRaw(parentId: Int): List<FolderEntity>

    @Query("SELECT * FROM folders WHERE isDeleted = 0 AND name LIKE :query ORDER BY name")
    fun searchFoldersFlow(query: String): Flow<List<FolderEntity>>

    @Query("""
        SELECT f.*, 
               (SELECT COUNT(*) FROM folders sf WHERE sf.parentFolderId = f.id AND sf.isDeleted = 0) AS subfolderCount,
               (SELECT COUNT(*) FROM note n WHERE n.folderId = f.id AND n.isDeleted = 0) AS noteCount
        FROM folders f
        WHERE f.isDeleted = 0 AND f.name LIKE :query
    """)
    fun searchFolderSummariesFlow(query: String): Flow<List<FolderWithNoteCount>>

    @Transaction
    @Query("""
        SELECT f.*, 
               (SELECT COUNT(*) FROM folders AS sub WHERE sub.parentFolderId = f.id AND sub.isDeleted = 0) AS subfolderCount,
               (SELECT COUNT(*) FROM note AS n WHERE n.folderId = f.id AND n.isDeleted = 0) AS noteCount
        FROM folders AS f
        WHERE f.id = :folderId
        LIMIT 1
    """)
    fun getFolderSummaryFlow(folderId: Int): Flow<FolderWithNoteCount>
}