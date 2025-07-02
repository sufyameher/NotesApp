package com.example.notesapp.note

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "note")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val createdDate: Long = System.currentTimeMillis(),
    val modifiedDate: Long = System.currentTimeMillis(),
    val folderId: Int? = null,
    val isEdited: Boolean = false,
    val isDeleted: Boolean = false


)
