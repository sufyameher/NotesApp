package com.example.notesapp.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import com.example.notesapp.common.Helper.hideKeyboard
import com.example.notesapp.common.gone
import com.example.notesapp.common.onClick
import com.example.notesapp.common.setupAdapter
import com.example.notesapp.common.show
import com.example.notesapp.common.showKeyboardAndFocus
import com.example.notesapp.common.visibleIfNotEmpty
import com.example.notesapp.databinding.ActivitySearchBinding
import com.example.notesapp.folder.adapter.FolderWithInfoAdapter
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.folder.ui.FolderActivity
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.MainActivityViewModel
import com.example.notesapp.note.NoteAdapter
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val folderActivityViewModel: FolderActivityViewModel by viewModels()

    private lateinit var folderWithInfoAdapter: FolderWithInfoAdapter
    private lateinit var noteAdapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSearchBar()
        setupRecyclerViews()
        setupSearchListeners()
        setupCancelButton()
    }

    private fun setupSearchBar() {
        binding.etSearch.showKeyboardAndFocus()
    }

    private fun setupRecyclerViews() {
        folderWithInfoAdapter = FolderWithInfoAdapter(
            folders = emptyList(),
            onFolderClick = { openSubFolder(it.folder.id, it.folder.name) },
            onFolderLongClick = { /* optional */ }
        )

        noteAdapter = NoteAdapter(
            onItemClick = { AddNoteActivity.launch(this, noteId = it.id) },
            onNoteLongClick = { /* optional */ }
        )

        binding.rvNotes.setupAdapter(noteAdapter)
        binding.rvFolders.setupAdapter(folderWithInfoAdapter)
    }

    private fun setupSearchListeners() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().trim()

            if (query.isNotEmpty()) {
                binding.ivClear.show()

                folderActivityViewModel.searchFolderSummaries(query)
                folderActivityViewModel.folderSearchResults.observe(this) { folders ->
                    folders.forEach {
                        Timber.d("Folder: ${it.folder.name}, Subfolders: ${it.subfolderCount}, Notes: ${it.noteCount}")
                    }
                    folderWithInfoAdapter.updateFolders(folders)
                    binding.tvFolders.visibleIfNotEmpty(folders)
                    binding.rvFolders.visibleIfNotEmpty(folders)
                }

                mainActivityViewModel.searchNotes(query)
                mainActivityViewModel.searchResults.observe(this) { notes ->
                    noteAdapter.submitList(notes)
                    binding.tvNotes.visibleIfNotEmpty(notes)
                    binding.rvNotes.visibleIfNotEmpty(notes)
                }

            } else {
                clearSearchResults()
            }
        }

        binding.ivClear.onClick {
            binding.etSearch.setText("")
        }
    }

    private fun clearSearchResults() {
        binding.ivClear.gone()
        folderWithInfoAdapter.updateFolders(emptyList())
        noteAdapter.submitList(emptyList())

        binding.tvFolders.gone()
        binding.rvFolders.gone()
        binding.tvNotes.gone()
        binding.rvNotes.gone()
    }

    private fun setupCancelButton() {
        binding.btnCancel.onClick {
            binding.etSearch.hideKeyboard()
            finish()
        }
    }

    private fun openSubFolder(folderId: Int, folderName: String) {
        FolderActivity.start(this, folderId, folderName)
    }

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SearchActivity::class.java)
            context.startActivity(intent)
        }
    }

}