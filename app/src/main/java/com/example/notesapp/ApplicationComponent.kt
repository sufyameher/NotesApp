package com.example.notesapp

import android.content.Context
import com.example.notesapp.db.AppDatabase
import com.example.notesapp.folder.data.FolderDao
import com.example.notesapp.folder.data.FolderRepository
import com.example.notesapp.note.NoteDao
import com.example.notesapp.note.NoteRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ApplicationComponent {
    @Provides
    fun provideNoteRepository(noteDao: NoteDao): NoteRepository =
        NoteRepository(noteDao)

    @Provides
    fun provideFolderRepository(folderDao: FolderDao): FolderRepository =
        FolderRepository(folderDao)

    @Provides
    fun provideNoteDao(appDatabase: AppDatabase): NoteDao =
        appDatabase.noteDao()

    @Provides
    fun provideFolderDao(appDatabase: AppDatabase): FolderDao =
        appDatabase.folderDao()

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)
}
