package com.example.notesapp.folder

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val createdDate: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val parentFolderId: Int? = null

)

