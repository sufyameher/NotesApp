package com.example.notesapp.folder.ui

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import com.example.notesapp.common.Helper
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.R
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.common.multiselect.MultiSelectActionHandler
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.folder.data.FolderViewModel
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteViewModel
import com.example.notesapp.common.ToolbarHelper
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showMoveConfirmDialog
import com.example.notesapp.util.showNewFolderDialog
import com.example.notesapp.util.showNoteActions

class FolderActivity : AppCompatActivity() {

    private lateinit var binding: FolderActivityBinding
    private lateinit var folderViewModel: FolderViewModel
    private val noteViewModel: NoteViewModel by viewModels()
    private var currentFolderId: Int = -1
    private var originalFolderName: String = ""
    private var editedFolderName: String = ""

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var subfolderAdapter: FolderAdapter

    private lateinit var multiSelectActionHandler: MultiSelectActionHandler


    private var isMultiSelectMode = false
    private val selectedNoteIds = mutableSetOf<Int>()
    private val selectedFolderIds = mutableSetOf<Int>()


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

        binding.ivCloseSelection.setOnClickListener {
            exitMultiSelectMode()
        }

        binding.ivSelectionMenu.setOnClickListener {
            showSelectionMenuPopup(it)
        }


        setupToolbar()
        setupRenameFolderUI()
        setupAdapters()
        observeFolders()
        observeNotes()
        observeViewMode()
        setupFab()

        noteViewModel.sortNotesInFolder(currentFolderId, "DATE_CREATED", "DESC")

        multiSelectActionHandler = MultiSelectActionHandler(
            context = this,
            lifecycleOwner = this,
            noteViewModel = noteViewModel,
            folderViewModel = folderViewModel,
            fragmentManager = supportFragmentManager,
            layoutInflater = layoutInflater
        ) {
            exitMultiSelectMode()
        }

    }

    private fun handleMoveSelectedItemsAndFolders() {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleMove(selectedNotes, selectedFolders)
    }

    private fun handleCopySelectedItemsAndFolders() {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleCopy(selectedNotes, selectedFolders)
    }

    private fun handleDeleteSelectedItemsAndFolders() {
        val selectedNotes = latestNotes.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestSubfolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectActionHandler.handleDelete(selectedNotes, selectedFolders)
    }


    private fun showSelectionMenuPopup(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_multiselect, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_move -> handleMoveSelectedItemsAndFolders()
                    R.id.action_copy -> handleCopySelectedItemsAndFolders()
                    R.id.action_delete -> handleDeleteSelectedItemsAndFolders()
                    R.id.action_select_all -> handleSelectAllItemsAndFolders()
                }
                true
            }
        }.show()
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
        if (isMultiSelectMode) {
            exitMultiSelectMode()
        } else {
            handleFolderRenameIfNeeded()
            super.onBackPressed()
        }
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
            onItemClick = { note ->
                if (isMultiSelectMode) toggleNoteSelection(note)
                else openNoteEditor(note.id)
            },
            onNoteLongClick = {
                enableMultiSelectMode()
                toggleNoteSelection(it)
            }
        ).apply {
            isMultiSelectMode = false
            selectedIds = selectedNoteIds
            onSelectionToggle = { toggleNoteSelection(it) }
        }

        subfolderAdapter = FolderAdapter(
            folders = emptyList(),
            onFolderClick = { folder ->
                if (isMultiSelectMode) toggleFolderSelection(folder)
                else openSubFolder(folder.id, folder.name)
            },
            onFolderLongClick = {
                enableMultiSelectMode()
                toggleFolderSelection(it)
            }
        ).apply {
            isMultiSelectMode = false
            selectedIds = selectedFolderIds
            onSelectionToggle = { toggleFolderSelection(it) }
        }


        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)

            val noteSwipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val note = latestNotes.getOrNull(position)

                    if (note != null) {
                        showNoteActions(
                            context = this@FolderActivity,
                            fragmentManager = supportFragmentManager,
                            note = note,
                            getFolders = { callback ->
                                folderViewModel.allFolders.observe(this@FolderActivity) {
                                    callback(it)
                                }
                            },
                            onNoteUpdated = { noteViewModel.update(it) },
                            onNoteInserted = { noteViewModel.insert(it) }
                        )
                        noteAdapter.notifyItemChanged(position) // Reset swipe animation
                    } else {
                        noteAdapter.notifyItemChanged(position)
                    }
                }
            }

            ItemTouchHelper(noteSwipeCallback).attachToRecyclerView(binding.recyclerView)

        }

        binding.subfolderRecyclerView.apply {
            adapter = subfolderAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)

            val folderSwipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: androidx.recyclerview.widget.RecyclerView,
                    viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                    target: androidx.recyclerview.widget.RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val folder = latestSubfolders.getOrNull(position)

                    if (folder != null) {
                        folderViewModel.getFolderInfo(folder.id).observeOnce(this@FolderActivity) { info ->
                            showFolderActionSheet(folder, info)
                            subfolderAdapter.notifyItemChanged(position) // Reset swipe animation
                        }
                    } else {
                        subfolderAdapter.notifyItemChanged(position)
                    }
                }
            }

            ItemTouchHelper(folderSwipeCallback).attachToRecyclerView(this)
        }
    }

    private fun enableMultiSelectMode() {
        if (isMultiSelectMode) return

        isMultiSelectMode = true
        noteAdapter.isMultiSelectMode = true
        subfolderAdapter.isMultiSelectMode = true
        binding.topBar.root.visibility = View.GONE
        binding.selectionTopBar.visibility = View.VISIBLE
        updateSelectionCount()
        noteAdapter.notifyDataSetChanged()
        subfolderAdapter.notifyDataSetChanged()
    }


    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedNoteIds.clear()
        selectedFolderIds.clear()
        noteAdapter.isMultiSelectMode = false
        subfolderAdapter.isMultiSelectMode = false
        noteAdapter.selectedIds = emptySet()
        subfolderAdapter.selectedIds = emptySet()
        noteAdapter.notifyDataSetChanged()
        subfolderAdapter.notifyDataSetChanged()
        binding.topBar.root.visibility = View.VISIBLE
        binding.selectionTopBar.visibility = View.GONE
    }


    private fun toggleNoteSelection(note: NoteEntity) {
        if (selectedNoteIds.contains(note.id)) selectedNoteIds.remove(note.id)
        else selectedNoteIds.add(note.id)
        noteAdapter.selectedIds = selectedNoteIds
        noteAdapter.notifyDataSetChanged()
        updateSelectionCount()

    }

    private fun toggleFolderSelection(folder: FolderEntity) {
        if (selectedFolderIds.contains(folder.id)) selectedFolderIds.remove(folder.id)
        else selectedFolderIds.add(folder.id)
        subfolderAdapter.selectedIds = selectedFolderIds
        subfolderAdapter.notifyDataSetChanged()
        updateSelectionCount()

    }

    private fun handleSelectAllItemsAndFolders() {
        selectedNoteIds.clear()
        selectedNoteIds.addAll(latestNotes.map { it.id })

        selectedFolderIds.clear()
        selectedFolderIds.addAll(latestSubfolders.map { it.id })

        noteAdapter.selectedIds = selectedNoteIds
        subfolderAdapter.selectedIds = selectedFolderIds

        updateSelectionCount()
        noteAdapter.notifyDataSetChanged()
        subfolderAdapter.notifyDataSetChanged()
    }


    private fun updateSelectionCount() {
        val count = selectedNoteIds.size + selectedFolderIds.size
        binding.tvSelectedCount.text = "$count Selected"
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
                        folderViewModel.refreshSubfolders(
                            targetFolder.id,
                            onlyIfVisible = currentFolderId
                        )
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
         Helper.updateSectionVisibility(
            binding = binding,
            hasSubfolders = latestSubfolders.isNotEmpty(),
            hasNotes = latestNotes.isNotEmpty()
        )
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
        start(this, folderId, name)
    }

    companion object {
        private const val INTENT_FOLDER_ID = "folder_id"
        private const val INTENT_FOLDER_NAME = "folder_name"

        fun start(
            context: Context,
            folderId: Int,
            folderName: String,
            isClearTop: Boolean = false
        ) {
            val intent = Intent(context, FolderActivity::class.java).apply {
                putExtra(INTENT_FOLDER_ID, folderId)
                putExtra(INTENT_FOLDER_NAME, folderName)
            }
            if (isClearTop) {
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
        }
    }



}