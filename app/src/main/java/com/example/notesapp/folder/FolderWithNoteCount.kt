package com.example.notesapp.folder

import androidx.room.Embedded

data class FolderWithNoteCount(
    @Embedded val folder: FolderEntity,
    val subfolderCount: Int,
    val noteCount: Int
)
