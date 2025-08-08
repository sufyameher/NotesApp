package com.example.notesapp.folder.model

import androidx.room.Embedded
import com.example.notesapp.folder.model.FolderEntity

data class FolderWithNoteCount(
    @Embedded val folder: FolderEntity,
    val subfolderCount: Int,
    val noteCount: Int
)