package com.example.notesapp.folder.data

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.notesapp.db.AppDatabase
import com.example.notesapp.folder.model.FolderWithNoteCount
import com.example.notesapp.note.NoteRepository
import kotlinx.coroutines.launch

class FolderViewModel(application: Application) : AndroidViewModel(application) {

    private val folderRepository: FolderRepository
    private val noteRepository: NoteRepository

    val subfolders = MutableLiveData<List<FolderEntity>>()
    val allFolders: LiveData<List<FolderEntity>>
    val activeFolders: LiveData<List<FolderEntity>>
    val deletedFolders: LiveData<List<FolderEntity>>

    init {
        val db = AppDatabase.getInstance(application)
        folderRepository = FolderRepository(db.folderDao())
        noteRepository = NoteRepository(db.noteDao())

        allFolders = folderRepository.getAllFolders()
        activeFolders = folderRepository.getActiveFolders()
        deletedFolders = folderRepository.deletedFolders
    }

    fun insert(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.insert(folder)
    }

    fun update(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.update(folder)
    }

    fun delete(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.delete(folder)
    }

    fun renameFolder(folderId: Int, newName: String) = viewModelScope.launch {
        folderRepository.getFolderById(folderId)?.let {
            folderRepository.update(it.copy(name = newName))
        }
    }

    fun softDeleteFolder(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.update(folder.copy(isDeleted = true))
    }

    fun deleteFolderAndNotes(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.update(folder.copy(isDeleted = true))
        val notes = noteRepository.getNotesByFolderIdRaw(folder.id)
        notes.forEach { noteRepository.update(it.copy(isDeleted = true)) }
    }

    fun getSubfolders(parentId: Int): LiveData<List<FolderEntity>> {
        return folderRepository.getSubfolders(parentId)
    }

    fun moveFolderToParent(folder: FolderEntity, targetParentId: Int?) = viewModelScope.launch {
        folderRepository.update(folder.copy(parentFolderId = targetParentId))
    }

    fun getFolderInfo(folderId: Int): LiveData<String> {
        return folderRepository.getFolderInfo(folderId)
    }

    fun getFolderLive(folderId: Int): LiveData<FolderEntity?> {
        return folderRepository.getFolderLive(folderId)
    }


//    fun moveNotesBetweenFolders(fromFolderId: Int, toFolderId: Int) = viewModelScope.launch {
//        val notes = noteRepository.getNotesByFolderIdRaw(fromFolderId)
//        Log.d("FolderMove", "Moving ${notes.size} notes from $fromFolderId to $toFolderId")
//
//        notes.forEach { note ->
//            val updatedNote = note.copy(folderId = toFolderId)
//            Log.d("FolderMove", "Note ID ${note.id} -> folderId ${updatedNote.folderId}")
//            noteRepository.update(updatedNote)
//        }
//    }

//    fun copyAllNotesFromFolderTo(fromFolderId: Int, toFolderId: Int) = viewModelScope.launch {
//        val notes = noteRepository.getNotesByFolderIdRaw(fromFolderId)
//        notes.forEach { note ->
//            val copied = note.copy(id = 0, folderId = toFolderId)
//            noteRepository.insert(copied)
//        }
//    }

    fun searchFolderSummaries(query: String): LiveData<List<FolderWithNoteCount>> {
        return folderRepository.searchFolderSummaries(query)
    }

//    fun searchFolders(query: String): LiveData<List<FolderEntity>> {
//        return folderRepository.searchFolders(query)
//    }

    fun copyFolderWithContents(
        sourceFolder: FolderEntity,
        targetParentId: Int,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                copyFolderRecursively(sourceFolder, targetParentId)
                onDone()
            } catch (_: Exception) {
            }
        }
    }

    private suspend fun copyFolderRecursively(sourceFolder: FolderEntity, targetParentId: Int) {
        val newFolder = sourceFolder.copy(id = 0, parentFolderId = targetParentId)
        val newFolderId = folderRepository.copyFolder(newFolder)

        noteRepository.getNotesByFolderIdRaw(sourceFolder.id).forEach { note ->
            val copiedNote = note.copy(id = 0, folderId = newFolderId)
            noteRepository.insert(copiedNote)
        }

        folderRepository.getSubfoldersRaw(sourceFolder.id).forEach { subfolder ->
            copyFolderRecursively(subfolder, newFolderId)
        }
    }

    fun refreshSubfolders(parentId: Int, onlyIfVisible: Int? = null) {
        viewModelScope.launch {
            if (onlyIfVisible == null || onlyIfVisible == parentId) {
                val updated = folderRepository.getSubfoldersRaw(parentId)
                subfolders.postValue(updated)
            }
        }
    }



}