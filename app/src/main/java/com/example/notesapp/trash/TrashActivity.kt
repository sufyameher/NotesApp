package com.example.notesapp.trash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.databinding.ActivityRecentlyDeletedBinding
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.util.showMoveConfirmDialog
import com.example.notesapp.folder.FolderViewModel
import com.example.notesapp.note.NoteViewModel

class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentlyDeletedBinding
    private val noteViewModel: NoteViewModel by viewModels()
    private val folderViewModel: FolderViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentlyDeletedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        folderViewModel.activeFolders.observe(this) { /* Optional cache or UI update */ }

        setupToolbar()
        setupRecyclerView()
        observeDeletedNotes()
    }

    private fun setupToolbar() {
        binding.ivBack.setOnClickListener { finish() }
        binding.ivDeleteAll.setOnClickListener { showClearDeletedConfirmation() }
    }

    private fun setupRecyclerView() {
        setupAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@TrashActivity)
            adapter = this@TrashActivity.adapter
        }
    }

    private fun setupAdapter() {
        adapter = NoteAdapter(
            onItemClick = { /* Optional: preview deleted note */ },
            onNoteLongClick = { note -> showDeletedNoteOptions(note) }
        )
    }

    private fun showDeletedNoteOptions(note: NoteEntity) {
        TrashNoteActionBottomSheet(
            note = note,
            onRecover = {
                noteViewModel.recover(it)
                toast("Note recovered")
            },
            onMoveTo = { selectedNote ->
                handleMoveTo(selectedNote)
            },
            onDeleteForever = {
                noteViewModel.permanentlyDelete(it)
                toast("Note deleted permanently")
            }
        ).show(supportFragmentManager, "DeletedNoteSheet")
    }

    private fun handleMoveTo(selectedNote: NoteEntity) {
        val folders = folderViewModel.activeFolders.value.orEmpty()
        val availableFolders = folders.filter { it.id != selectedNote.folderId }

        if (availableFolders.isEmpty()) {
            toast("No folders available")
            return
        }

        showMoveConfirmDialog(
            context = this,
            inflater = layoutInflater,
            folders = availableFolders,
            currentFolderId = selectedNote.folderId ?: -1,
            titleText = "Move to",
            ) { targetFolder ->
            val movedNote = selectedNote.copy(
                folderId = targetFolder?.id,
                isDeleted = false
            )
            noteViewModel.update(movedNote)
            toast("Note moved")

        }
    }

    private fun observeDeletedNotes() {
        noteViewModel.deletedNotes.observe(this) { notes ->
            adapter.submitList(notes)
        }
    }

    private fun showClearDeletedConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Recently Deleted?")
            .setMessage("Deleted items will be permanently removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                noteViewModel.permanentlyDeleteAll()
            }
            .show()
    }
}
