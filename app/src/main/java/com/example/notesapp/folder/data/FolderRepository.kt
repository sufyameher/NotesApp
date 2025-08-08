package com.example.notesapp.folder.data

import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.model.FolderWithNoteCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class FolderRepository@Inject constructor(
    private val folderDao: FolderDao
) {
    fun getAllFolders(): Flow<List<FolderEntity>> = folderDao.getAllFoldersFlow()
    fun getActiveFolders(): Flow<List<FolderEntity>> = folderDao.getActiveFoldersFlow()
    fun getSubfolders(parentId: Int): Flow<List<FolderEntity>> = folderDao.getSubfoldersFlow(parentId)

    val deletedFolders: Flow<List<FolderEntity>> = folderDao.getDeletedFoldersFlow()

    suspend fun insert(folder: FolderEntity) {
        folderDao.insertFolder(folder)
    }

    suspend fun insertDummyData(folder: FolderEntity): Int {
        return folderDao.insert(folder).toInt()
    }

    fun getFolderLive(folderId: Int): Flow<FolderEntity?> {
        return folderDao.getFolderLiveFlow(folderId)
    }

    suspend fun delete(folder: FolderEntity) {
        folderDao.delete(folder)
    }

    suspend fun deleteAllFolders() {
        folderDao.deleteAllFolders()
    }

    suspend fun update(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun getFolderById(folderId: Int): FolderEntity? {
        return folderDao.getFolderById(folderId)
    }

    fun searchFolderSummaries(query: String): Flow<List<FolderWithNoteCount>> {
        return folderDao.searchFolderSummariesFlow("%$query%")
    }

    fun searchFolders(query: String): Flow<List<FolderEntity>> {
        return folderDao.searchFoldersFlow("%$query%")
    }

    fun getFolderInfo(folderId: Int): Flow<String> {
        return folderDao.getFolderSummaryFlow(folderId).map { folderWithCount ->
            val sub = folderWithCount.subfolderCount
            val notes = folderWithCount.noteCount
            "$sub subfolder${if (sub != 1) "s" else ""} · $notes note${if (notes != 1) "s" else ""}"

        }
    }

    suspend fun getSubfoldersRaw(parentId: Int): List<FolderEntity> {
        return folderDao.getSubfoldersRaw(parentId)
    }

    suspend fun copyFolder(folder: FolderEntity): Int {
        return folderDao.insertFolderToCopy(folder).toInt()
    }
}