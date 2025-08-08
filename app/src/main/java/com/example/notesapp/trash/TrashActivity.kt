package com.example.notesapp.trash

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.common.getNoteSwipeForTrash
import com.example.notesapp.common.onClick
import com.example.notesapp.common.setupAdapter
import com.example.notesapp.databinding.ActivityRecentlyDeletedBinding
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.util.showMoveConfirmDialog
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.note.MainActivityViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrashActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecentlyDeletedBinding
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private val trashViewModel: TrashViewModel by viewModels()
    private val folderActivityViewModel: FolderActivityViewModel by viewModels()
    private lateinit var adapter: NoteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecentlyDeletedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        folderActivityViewModel.activeFolders.observe(this) { /* Optional cache or UI update */ }

        setupToolbar()
        setupRecyclerView()
        observeDeletedNotes()
        trashViewModel.loadDeletedNotes()
    }

    private fun setupToolbar() {
        binding.ivBack.onClick { finish() }
        binding.ivDeleteAll.onClick { showClearDeletedConfirmation() }
    }

    private fun setupRecyclerView() {
        setupAdapter()
        binding.recyclerView.setupAdapter(adapter)

        val swipeHandler = getNoteSwipeForTrash(adapter) { note ->
            showDeletedNoteOptions(note)
        }

        swipeHandler.attachToRecyclerView(binding.recyclerView)
    }

    private fun setupAdapter() {
        adapter = NoteAdapter(
            onItemClick = { /* Optional: preview deleted note */ },
            onNoteLongClick = { /* Do nothing */ }
        )
    }

    private fun showDeletedNoteOptions(note: NoteEntity) {
        TrashNoteActionBottomSheet(
            note = note,
            onRecover = {
                trashViewModel.recover(it)
                toast("Note recovered")
            },
            onMoveTo = { selectedNote ->
                handleMoveTo(selectedNote)
            },
            onDeleteForever = {
                trashViewModel.permanentlyDelete(it)
                toast("Note deleted permanently")
            }
        ).show(supportFragmentManager, "DeletedNoteSheet")
    }

    private fun handleMoveTo(selectedNote: NoteEntity) {
        val folders = folderActivityViewModel.activeFolders.value.orEmpty()
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
                folderId = targetFolder?.id ?: 0,
                isDeleted = false
            )
            mainActivityViewModel.update(movedNote)
            toast("Note moved")

        }
    }

    private fun observeDeletedNotes() {
        trashViewModel.deletedNotes.observe(this) { notes ->
            adapter.submitList(notes)
        }
    }

    private fun showClearDeletedConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Clear Recently Deleted?")
            .setMessage("Deleted items will be permanently removed.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                trashViewModel.permanentlyDeleteAll()
            }.show()
    }

    companion object {
        fun start(context: android.content.Context) {
            val intent = android.content.Intent(context, TrashActivity::class.java)
            context.startActivity(intent)
        }
    }
}
