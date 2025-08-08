package com.example.notesapp.folder.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.notesapp.common.LiveDataHost
import com.example.notesapp.common.LiveDataHostNullable
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.model.FolderWithNoteCount
import com.example.notesapp.note.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class FolderActivityViewModel @Inject constructor(
    private var folderRepository: FolderRepository,
    private var noteRepository: NoteRepository
) : ViewModel() {

    val allFolders = LiveDataHost(viewModelScope, emptyList<FolderEntity>())
    val activeFolders = LiveDataHost(viewModelScope, emptyList<FolderEntity>())
    val deletedFolders = LiveDataHost(viewModelScope, emptyList<FolderEntity>())
    val subfolders = LiveDataHost(viewModelScope, emptyList<FolderEntity>())
    val folderInfo = LiveDataHost(viewModelScope, "")
    val folderById = LiveDataHostNullable<FolderEntity?>(viewModelScope)
    val folderSearchResults = LiveDataHost(viewModelScope, emptyList<FolderWithNoteCount>())

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
            Timber.d("Renaming folder id=$folderId to $newName")
            folderRepository.update(it.copy(name = newName))
        }
    }

    fun loadInitialFolders() {
        viewModelScope.launch {
            folderRepository.getAllFolders().collect {
                allFolders.postValue(it)
            }
        }
        viewModelScope.launch {
            folderRepository.getActiveFolders().collect {
                activeFolders.postValue(it)
            }
        }
        viewModelScope.launch {
            folderRepository.deletedFolders.collect {
                deletedFolders.postValue(it)
            }
        }
    }


    fun deleteFolderAndNotes(folder: FolderEntity) = viewModelScope.launch {
        folderRepository.update(folder.copy(isDeleted = true))

        noteRepository.getNotesByFolderIdRaw(folder.id)
            .first()
            .forEach { note ->
                noteRepository.update(note.copy(isDeleted = true))
            }

        activeFolders(folderRepository.getActiveFolders())
        deletedFolders(folderRepository.deletedFolders)
    }


    fun getSubfolders(parentId: Int) {
        subfolders(folderRepository.getSubfolders(parentId))
    }

    fun moveFolderToParent(folder: FolderEntity, targetParentId: Int?) = viewModelScope.launch {
        folderRepository.update(folder.copy(parentFolderId = targetParentId))
    }

    fun getFolderInfo(folderId: Int) {
        folderInfo(folderRepository.getFolderInfo(folderId))
    }

    fun getFolderLive(folderId: Int) {
        folderById(folderRepository.getFolderLive(folderId))
    }

    fun searchFolderSummaries(query: String) {
        folderSearchResults(folderRepository.searchFolderSummaries(query))
    }

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

        val notes = noteRepository.getNotesByFolderIdRaw(sourceFolder.id).first()
        notes.forEach { note ->
            val copiedNote = note.copy(id = 0, folderId = newFolderId)
            noteRepository.insert(copiedNote)
        }

        val subfolders = folderRepository.getSubfoldersRaw(sourceFolder.id)
        subfolders.forEach { subfolder ->
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