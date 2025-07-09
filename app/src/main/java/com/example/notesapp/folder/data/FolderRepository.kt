package com.example.notesapp.folder.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import com.example.notesapp.folder.model.FolderWithNoteCount

class FolderRepository(private val folderDao: FolderDao) {

    fun getAllFolders(): LiveData<List<FolderEntity>> = folderDao.getAllFolders()
    fun getActiveFolders(): LiveData<List<FolderEntity>> = folderDao.getActiveFolders()
    fun getSubfolders(parentId: Int): LiveData<List<FolderEntity>> = folderDao.getSubfolders(parentId)

    val deletedFolders: LiveData<List<FolderEntity>> = folderDao.getDeletedFolders()


    suspend fun insert(folder: FolderEntity) {
        folderDao.insertFolder(folder)
    }

    suspend fun delete(folder: FolderEntity) {
        folderDao.delete(folder)
    }

    suspend fun update(folder: FolderEntity) {
        folderDao.updateFolder(folder)
    }

    suspend fun getFolderById(folderId: Int): FolderEntity? {
        return folderDao.getFolderById(folderId)
    }

    fun searchFolderSummaries(query: String): LiveData<List<FolderWithNoteCount>> {
        return folderDao.searchFolderSummaries("%$query%")
    }

    fun searchFolders(query: String): LiveData<List<FolderEntity>> {
        return folderDao.searchFolders("%$query%")
    }

    fun getFolderInfo(folderId: Int): LiveData<String> {
        return folderDao.getFolderSummary(folderId).map { folderWithCount ->
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