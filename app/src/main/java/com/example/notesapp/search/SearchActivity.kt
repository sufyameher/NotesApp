package com.example.notesapp.search

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doOnTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.databinding.ActivitySearchBinding
import com.example.notesapp.folder.ui.FolderActivity
import com.example.notesapp.folder.data.FolderViewModel
import com.example.notesapp.folder.adapter.FolderWithInfoAdapter
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteViewModel

class SearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySearchBinding
    private val noteViewModel: NoteViewModel by viewModels()
    private val folderViewModel: FolderViewModel by viewModels()

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
        binding.etSearch.requestFocus()
        binding.etSearch.postDelayed({
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
    }

    private fun setupRecyclerViews() {
        folderWithInfoAdapter = FolderWithInfoAdapter(
            folders = emptyList(),
            onFolderClick = { openSubFolder(it.folder.id, it.folder.name) },
            onFolderLongClick = { /* optional */ }
        )

        noteAdapter = NoteAdapter(
            onItemClick = {
                val intent = Intent(this, AddNoteActivity::class.java)
                intent.putExtra("note_id", it.id)
                startActivity(intent)
            },
            onNoteLongClick = { /* optional */ }
        )

        binding.rvFolders.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = folderWithInfoAdapter
        }

        binding.rvNotes.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = noteAdapter
        }
    }

    private fun setupSearchListeners() {
        binding.etSearch.doOnTextChanged { text, _, _, _ ->
            val query = text.toString().trim()

            if (query.isNotEmpty()) {
                binding.ivClear.visibility = View.VISIBLE

                folderViewModel.searchFolderSummaries(query).observe(this) { folders ->
                    folders.forEach {
                        Log.d(
                            "FolderQueryDebug",
                            "Folder: ${it.folder.name}, Subfolders: ${it.subfolderCount}, Notes: ${it.noteCount}"
                        )
                    }
                    folderWithInfoAdapter.updateFolders(folders)
                    binding.tvFolders.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
                    binding.rvFolders.visibility = if (folders.isEmpty()) View.GONE else View.VISIBLE
                }

                noteViewModel.searchNotes(query).observe(this) { notes ->
                    noteAdapter.submitList(notes)
                    binding.tvNotes.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
                    binding.rvNotes.visibility = if (notes.isEmpty()) View.GONE else View.VISIBLE
                }

            } else {
                clearSearchResults()
            }
        }

        binding.ivClear.setOnClickListener {
            binding.etSearch.setText("")
        }
    }

    private fun clearSearchResults() {
        binding.ivClear.visibility = View.GONE
        folderWithInfoAdapter.updateFolders(emptyList())
        noteAdapter.submitList(emptyList())

        binding.tvFolders.visibility = View.GONE
        binding.rvFolders.visibility = View.GONE
        binding.tvNotes.visibility = View.GONE
        binding.rvNotes.visibility = View.GONE
    }

    private fun setupCancelButton() {
        binding.btnCancel.setOnClickListener {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            finish()
        }
    }

    private fun openSubFolder(folderId: Int, folderName: String) {
        FolderActivity.start(this, folderId, folderName)
    }
}