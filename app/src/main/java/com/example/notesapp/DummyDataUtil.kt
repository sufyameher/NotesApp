package com.example.notesapp

import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.data.FolderRepository
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteRepository
import kotlinx.coroutines.flow.first
import timber.log.Timber

object DummyDataUtil {
    suspend fun createDummyNotesAndFolders(
        folderRepository: FolderRepository,
        noteRepository: NoteRepository
    ): Pair<Boolean, List<NoteEntity>> {

        noteRepository.deleteAllNotes()
        folderRepository.deleteAllFolders()

        val existingFolders = folderRepository.getAllFolders().first()
        val existingNotes = noteRepository.getAllNotes().first()

        Timber.d("Folders: ${existingFolders.size}, Notes: ${existingNotes.size}")
        if (existingFolders.isNotEmpty() || existingNotes.isNotEmpty()) {
            return Pair(false, emptyList()) // Safety check
        }

        val noOfFolders = 5
        val notesPerFolder = 3
        val createdNotes = mutableListOf<NoteEntity>()

        repeat(noOfFolders) { folderIndex ->
            val folder = FolderEntity(name = "Folder ${folderIndex + 1}")
            val folderId = folderRepository.insertDummyData(folder)

            repeat(notesPerFolder) { noteIndex ->
                val now = System.currentTimeMillis()
                val note = NoteEntity(
                    title = "Note ${folderIndex + 1}.${noteIndex + 1}",
                    description = "This is the description of Note ${folderIndex + 1}.${noteIndex + 1}",
                    createdDate = now,
                    modifiedDate = now,
                    folderId = folderId,
                    isEdited = false,
                    isDeleted = false,
                    isPinned = false,
                    imageUris = emptyList()
                )
                noteRepository.insertDummyData(note)
                createdNotes.add(note)
            }
        }

        repeat(5) { noteIndex ->
            val now = System.currentTimeMillis()
            val rootNote = NoteEntity(
                title = "Root Note ${noteIndex + 1}",
                description = "This is a root note not in any folder",
                createdDate = now,
                modifiedDate = now,
                folderId = 0,
                isEdited = false,
                isDeleted = false,
                isPinned = false,
                imageUris = emptyList()
            )
            noteRepository.insertDummyData(rootNote)
            createdNotes.add(rootNote)
        }
        return Pair(true, createdNotes)
    }
}
