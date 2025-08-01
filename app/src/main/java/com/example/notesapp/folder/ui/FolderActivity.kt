package com.example.notesapp.folder.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.R
import com.example.notesapp.common.Helper
import com.example.notesapp.common.Helper.hideKeyboard
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.showKeyboard
import com.example.notesapp.common.ToolbarHelper
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.common.getFolderActivityNoteSwipeCallback
import com.example.notesapp.common.getFolderActivitySubfolderSwipeCallback
import com.example.notesapp.common.multiselect.MultiSelectActionHandler
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.folder.data.FolderViewModel
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteViewModel
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showNewFolderDialog

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

        initViewModels()
        initIntentData()
        observeCurrentFolder()
        setupToolbar()
        setupAdapters()
        setupFab()
        setupClickListeners()
        observeFolders()
        observeNotes()
        observeViewMode()
        setupMultiSelectHandler()
        //noteViewModel.sortNotesInFolder(currentFolderId, "DATE_CREATED", "DESC")
    }

    private fun initViewModels() {
        folderViewModel = ViewModelProvider(this)[FolderViewModel::class.java]
    }

    private fun initIntentData() {
        currentFolderId = intent.getIntExtra("folder_id", -1)
    }

    private fun observeCurrentFolder() {
        folderViewModel.getFolderLive(currentFolderId).observe(this) { folder ->
            folder?.let {
                originalFolderName = it.name
                editedFolderName = it.name
                binding.topBar.tvTopBarTitle.text = it.name
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivCloseSelection.setOnClickListener {
            exitMultiSelectMode() }
        binding.ivSelectionMenu.setOnClickListener {
            showSelectionMenuPopup(it) }
    }

    private fun setupMultiSelectHandler() {
        multiSelectActionHandler = MultiSelectActionHandler(
            context = this,
            lifecycleOwner = this,
            noteViewModel = noteViewModel,
            folderViewModel = folderViewModel,
            fragmentManager = supportFragmentManager,
            layoutInflater = layoutInflater,
            exitMultiSelectMode = { exitMultiSelectMode() },
            refreshRootFoldersUI = { updatedSubfolders ->
                subfolderAdapter.submitList(updatedSubfolders)
                latestSubfolders = updatedSubfolders
                updateSectionVisibility()
            }
        )
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
            tvTopBarTitle.setOnClickListener { setupRenameFolder() }
            ivNavToggle.setImageResource(R.drawable.ic_arrow_back)
            ivNavToggle.setOnClickListener { handleFolderRenameIfNeeded()
                onBackPressedDispatcher.onBackPressed() }
        }

        ToolbarHelper.setup(
            activity = this,
            toolbarBinding = binding.topBar,
            viewModel = noteViewModel,
            title = originalFolderName,
            onClickNewFolder = {
                showNewFolderDialog(this, currentFolderId) { newFolder ->
                    folderViewModel.insert(newFolder)

                    // ✅ Manually add the new folder to the current subfolder list and update the adapter
//                    latestSubfolders = latestSubfolders + newFolder
//                    subfolderAdapter.submitList(latestSubfolders)
//                    updateSectionVisibility()
                }
            },
            onClickSort = { showSortBottomSheet() }
        )
    }

    private fun setupRenameFolder() {
        val tvTopBarTitle = binding.topBar.tvTopBarTitle
        val etTopBarTitle = binding.topBar.etTopBarTitle

        etTopBarTitle.setText(originalFolderName)

        tvTopBarTitle.visibility = View.GONE
        etTopBarTitle.visibility = View.VISIBLE
        etTopBarTitle.requestFocus()
        etTopBarTitle.setSelection(etTopBarTitle.text.length)
        etTopBarTitle.showKeyboard()

        etTopBarTitle.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val newName = etTopBarTitle.text.toString().trim()
                if (newName.isNotEmpty() && newName != originalFolderName) {
                    folderViewModel.renameFolder(currentFolderId, newName)
                    tvTopBarTitle.text = newName
                    originalFolderName = newName
                    editedFolderName = newName
                }

                etTopBarTitle.hideKeyboard()
                etTopBarTitle.visibility = View.GONE
                tvTopBarTitle.visibility = View.VISIBLE
                true
            } else {
                false
            }
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
        val newName = binding.topBar.etTopBarTitle.text.toString().trim()
        if (newName.isNotEmpty() && newName != originalFolderName) {
            folderViewModel.renameFolder(currentFolderId, newName)
            originalFolderName = newName
            editedFolderName = newName
        }
    }

    private fun setupAdapters() {
        setupNoteAdapter()
        setupSubfolderAdapter()
        setupNoteRecyclerView()
        setupSubfolderRecyclerView()
    }

    private fun setupNoteAdapter() {
        noteAdapter = NoteAdapter(
            onItemClick = { note ->
                if (isMultiSelectMode) toggleNoteSelection(note)
                else openNoteEditor(note.id)
            },
            onNoteLongClick = {
                enableMultiSelectMode()
            }
        ).apply {
            isMultiSelectMode = false
            selectedIds = selectedNoteIds
            onSelectionToggle = { toggleNoteSelection(it) }
        }
    }

    private fun setupSubfolderAdapter() {
        subfolderAdapter = FolderAdapter(
            onFolderClick = { folder ->
                if (isMultiSelectMode) toggleFolderSelection(folder)
                else openSubFolder(folder.id, folder.name)
            },
            onFolderLongClick = {
                enableMultiSelectMode()
            }
        ).apply {
            isMultiSelectMode = false
            selectedIds = selectedFolderIds
            onSelectionToggle = { toggleFolderSelection(it) }
        }
    }

    private fun setupNoteRecyclerView() {
        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)
            ItemTouchHelper(
                getFolderActivityNoteSwipeCallback(
                    activity = this@FolderActivity,
                    latestNotes = { latestNotes },
                    noteAdapter = noteAdapter,
                    noteViewModel = noteViewModel,
                    folderViewModel = folderViewModel
                )
            ).attachToRecyclerView(this)
        }
    }

    private fun setupSubfolderRecyclerView() {
        binding.subfolderRecyclerView.apply {
            adapter = subfolderAdapter
            layoutManager = LinearLayoutManager(this@FolderActivity)
            ItemTouchHelper(
                getFolderActivitySubfolderSwipeCallback(
                    activity = this@FolderActivity,
                    latestSubfolders = { latestSubfolders },
                    folderAdapter = subfolderAdapter,
                    folderViewModel = folderViewModel,
                    currentFolderId = currentFolderId
                )
            ).attachToRecyclerView(this)
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

    override fun onResume() {
        super.onResume()
        folderViewModel.refreshSubfolders(currentFolderId)
    }

    private fun observeFolders() {
        folderViewModel.getSubfolders(currentFolderId).observe(this) { subfolders ->
            latestSubfolders = subfolders
            subfolderAdapter.submitList(subfolders)
            updateSectionVisibility()
        }

//        folderViewModel.subfolders.observe(this) { subfolders ->
//            latestSubfolders = subfolders
//            subfolderAdapter.submitList(subfolders)
//            updateSectionVisibility()
//        }
    }

    private fun observeNotes() {
        noteViewModel.getNotesByFolderId(currentFolderId)
        noteViewModel.folderNotes.observe(this) { notes ->
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
            AddNoteActivity.launch(this, folderId = currentFolderId)
        }
    }


    private fun openNoteEditor(noteId: Int) {
        AddNoteActivity.launch(this, noteId = noteId)
    }


    private fun showSortBottomSheet() {
        SortBottomSheet.show(supportFragmentManager) { sortBy, order ->
            noteViewModel.sortNotesInFolder(currentFolderId, sortBy, order)
            noteViewModel.folderSortedNotes.observe(this) { sortedNotes ->
                noteAdapter.submitList(sortedNotes)
                latestNotes = sortedNotes
                updateSectionVisibility()
            }
        }
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