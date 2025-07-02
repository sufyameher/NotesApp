package com.example.notesapp.folder

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.R
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.common.SortBottomSheet
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.util.ToolbarHelper
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showMoveConfirmDialog
import com.example.notesapp.util.showNewFolderDialog
import com.example.notesapp.util.showNoteActions
import com.example.notesapp.note.NoteViewModel

class FolderActivity : AppCompatActivity() {

    private lateinit var binding: FolderActivityBinding
    private lateinit var folderViewModel: FolderViewModel
    private val noteViewModel: NoteViewModel by viewModels()
    private var currentFolderId: Int = -1
    private var originalFolderName: String = ""
    private var editedFolderName: String = ""

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var subfolderAdapter: FolderAdapter

    private var latestNotes: List<NoteEntity> = emptyList()
    private var latestSubfolders: List<FolderEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FolderActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentFolderId = intent.getIntExtra("folder_id", -1)
        val folderName = intent.getStringExtra("folder_name") ?: "Folder"
        originalFolderName = folderName
        editedFolderName = folderName

        folderViewModel = ViewModelProvider(this)[FolderViewModel::class.java]

        setupToolbar()
        setupRenameFolderUI()
        setupAdapters()
        observeFolders()
        observeNotes()
        observeViewMode()
        setupFab()

        noteViewModel.sortNotesInFolder(currentFolderId, "DATE_CREATED", "DESC")
    }

    private fun setupToolbar() {
        with(binding.topBar) {
            tvTopBarTitle.text = originalFolderName
            tvTopBarTitle.setTypeface(null, Typeface.BOLD)
            ivNavToggle.setImageResource(R.drawable.ic_arrow_back)
            ivNavToggle.setOnClickListener {
                handleFolderRenameIfNeeded()
                onBackPressedDispatcher.onBackPressed()
            }
        }

        ToolbarHelper.setup(
            activity = this,
            toolbarBinding = binding.topBar,
            viewModel = noteViewModel,
            title = originalFolderName,
            onClickNewFolder = {
                showNewFolderDialog(this, currentFolderId) { folderViewModel.insert(it) }
            },
            onClickSort = { showSortBottomSheet() }
        )
    }

    private fun setupRenameFolderUI() {
        val tvName = binding.tvSubfolders
        val etName = binding.etRenameFolder

        tvName.setOnClickListener {
            etName.setText(tvName.text)
            tvName.visibility = View.GONE
            etName.visibility = View.VISIBLE
            etName.requestFocus()
            etName.setSelection(etName.text.length)

            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etName, InputMethodManager.SHOW_IMPLICIT)
        }

        etName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = etName.text.toString().trim()
                if (newName.isNotEmpty()) {
                    editedFolderName = newName
                    tvName.text = newName
                    binding.topBar.tvTopBarTitle.text = newName
                }
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(etName.windowToken, 0)
                etName.visibility = View.GONE
                tvName.visibility = View.VISIBLE
                true
            } else false
        }
    }

    override fun onBackPressed() {
        handleFolderRenameIfNeeded()
        super.onBackPressed()
    }

    private fun handleFolderRenameIfNeeded() {
        val newName = editedFolderName.trim()
        if (newName.isNotEmpty() && newName != originalFolderName) {
            folderViewModel.renameFolder(currentFolderId, newName)
            toast("Folder renamed to \"$newName\"")
            originalFolderName = newName
        }
    }

    private fun setupAdapters() {
        noteAdapter = NoteAdapter(
            onItemClick = { openNoteEditor(it.id) },
            onNoteLongClick = { note ->
                showNoteActions(
                    context = this,
                    fragmentManager = supportFragmentManager,
                    note = note,
                    getFolders = { callback -> folderViewModel.allFolders.observe(this) { callback(it) } },
                    onNoteUpdated = { noteViewModel.update(it) },
                    onNoteInserted = { noteViewModel.insert(it) }
                )
            }
        )

        subfolderAdapter = FolderAdapter(
            folders = emptyList(),
            onFolderClick = { openSubFolder(it.id, it.name) },
            onFolderLongClick = { folder ->
                folderViewModel.getFolderInfo(folder.id).observeOnce(this) { info ->
                    showFolderActionSheet(folder, info)
                }
            }
        )

        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)
        }

        binding.subfolderRecyclerView.apply {
            adapter = subfolderAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)
        }
    }

    private fun showFolderActionSheet(folder: FolderEntity, folderInfo: String?) {
        FolderActionBottomSheet(
            folder = folder,
            folderInfo = folderInfo,
            onMoveTo = { handleMoveFolder(it) },
            onCopyTo = { handleCopyFolder(it) },
            onDelete = { confirmDeleteFolder(it) }
        ).show(supportFragmentManager, "FolderActionBottomSheet")
    }

    private fun handleMoveFolder(selectedFolder: FolderEntity) {
        folderViewModel.allFolders.observeOnce(this) { allFolders ->

            val parentFolder = allFolders.find { it.id == selectedFolder.parentFolderId }
            val available = allFolders.filter { it.id != selectedFolder.id }
            if (available.isEmpty()) return@observeOnce toast("No folders to move")


            showMoveConfirmDialog(
                context = this,
                inflater = layoutInflater,
                folders = available,
                currentFolderId = selectedFolder.id,
                startFromFolder = parentFolder,
                titleText = "Move to"
            ) { targetFolder ->
                targetFolder?.let {
                    folderViewModel.moveFolderToParent(selectedFolder, it.id)
                    folderViewModel.refreshSubfolders(currentFolderId)

                    toast("Folder moved to '${it.name}'")
                }
            }
        }
    }


    private fun handleCopyFolder(selectedFolder: FolderEntity) {
        folderViewModel.allFolders.observeOnce(this) { allFolders ->

            val parentFolder = allFolders.find { it.id == selectedFolder.parentFolderId }
            val available = allFolders.filter { it.id != selectedFolder.id }
            if (available.isEmpty()) return@observeOnce toast("No folders to move")

            showMoveConfirmDialog(
                context = this,
                inflater = layoutInflater,
                folders = available,
                currentFolderId = selectedFolder.id,
                startFromFolder = parentFolder,
                titleText = "Copy to"
            ) { targetFolder ->

                if (targetFolder == null) {
                    Log.d("COPY_UI", "User cancelled the copy dialog or nothing returned")
                } else {
                    Log.d("COPY_UI", "User selected folder: ${targetFolder.name}")
                    folderViewModel.copyFolderWithContents(selectedFolder, targetFolder.id) {
                        Log.d("COPY_UI", "Copy completed for ${selectedFolder.name}")
                        folderViewModel.refreshSubfolders(targetFolder.id, onlyIfVisible = currentFolderId)
                        toast("Folder copied to '${targetFolder.name}'")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        folderViewModel.refreshSubfolders(currentFolderId)
    }


    private fun confirmDeleteFolder(folderToDelete: FolderEntity) {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("This folder and its notes will be moved to Recently Deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                folderViewModel.deleteFolderAndNotes(folderToDelete)
                toast("Folder deleted successfully")
            }
            .show()
    }


    private fun observeFolders() {
        folderViewModel.getSubfolders(currentFolderId).observe(this) { subfolders ->
            latestSubfolders = subfolders
            subfolderAdapter.updateFolders(subfolders)
            updateSectionVisibility()
        }

        folderViewModel.subfolders.observe(this) { subfolders ->
            latestSubfolders = subfolders
            subfolderAdapter.updateFolders(subfolders)
            updateSectionVisibility()
        }
    }

    private fun observeNotes() {
        noteViewModel.getNotesByFolderId(currentFolderId).observe(this) { notes ->
            latestNotes = notes
            noteAdapter.submitList(notes)
            updateSectionVisibility()
        }
    }

    private fun updateSectionVisibility() {
        binding.apply {
            subfolderHeader.visibility = if (latestSubfolders.isNotEmpty()) View.VISIBLE else View.GONE
            tvSubfolders.visibility = if (latestSubfolders.isNotEmpty()) View.VISIBLE else View.GONE
            bottomSpacer.visibility = tvSubfolders.visibility
            tvNotes.visibility = if (latestNotes.isNotEmpty()) View.VISIBLE else View.GONE
            bottomSpacer1.visibility = tvNotes.visibility
        }
    }

    private fun observeViewMode() {
        noteViewModel.viewMode.observe(this) { mode ->
            binding.recyclerView.layoutManager = getLayoutManager(this, mode)
            binding.subfolderRecyclerView.layoutManager = getLayoutManager(this, mode)
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java).apply {
                putExtra("folder_id", currentFolderId)
            })
        }
    }

    private fun openNoteEditor(noteId: Int) {
        startActivity(Intent(this, AddNoteActivity::class.java).apply {
            putExtra("note_id", noteId)
        })
    }

    private fun showSortBottomSheet() {
        SortBottomSheet { sortBy, order ->
            noteViewModel.sortNotesInFolder(currentFolderId, sortBy, order)
        }.show(supportFragmentManager, "SortBottomSheet")
    }

    private fun openSubFolder(folderId: Int, name: String) {
        startActivity(Intent(this, FolderActivity::class.java).apply {
            putExtra("folder_id", folderId)
            putExtra("folder_name", name)
        })
    }
}
