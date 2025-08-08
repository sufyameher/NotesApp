package com.example.notesapp.note

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.notesapp.folder.model.FolderEntity


@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val folderId: Int = 0,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false,
    var isPinned: Boolean = false,
    val imageUris: List<String>? = emptyList())
