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
import com.example.notesapp.common.Helper.updateSectionVisibility
import com.example.notesapp.common.multiselect.MultiSelectHelper
import com.example.notesapp.common.SelectionUtils
import com.example.notesapp.common.ToolbarHelper
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.common.getFolderActivityNoteSwipeCallback
import com.example.notesapp.common.getFolderActivitySubfolderSwipeCallback
import com.example.notesapp.common.gone
import com.example.notesapp.common.multiselect.MultiSelectActionHandler
import com.example.notesapp.common.onClick
import com.example.notesapp.common.onClickWithView
import com.example.notesapp.common.show
import com.example.notesapp.common.showKeyboardAndFocus
import com.example.notesapp.common.visibleFolders
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.MainActivityViewModel
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showNewFolderDialog
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class FolderActivity : AppCompatActivity() {

    private lateinit var binding: FolderActivityBinding
    private lateinit var folderActivityViewModel: FolderActivityViewModel
    private val mainActivityViewModel: MainActivityViewModel by viewModels()

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
        folderActivityViewModel.loadInitialFolders()
    }

    private fun initViewModels() {
        folderActivityViewModel = ViewModelProvider(this)[FolderActivityViewModel::class.java]
    }

    private fun initIntentData() {
        currentFolderId = intent.getIntExtra("folder_id", -1)
    }

    private fun observeCurrentFolder() {
        folderActivityViewModel.getFolderLive(currentFolderId)
        folderActivityViewModel.folderById.observe(this) { folder ->
            folder?.let {
                originalFolderName = it.name
                editedFolderName = it.name
                binding.topBar.tvTopBarTitle.text = it.name
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivCloseSelection.onClick {
            exitMultiSelectMode() }
        binding.ivSelectionMenu.onClickWithView { view ->
            showSelectionMenuPopup(view)
        }
    }

    private fun setupMultiSelectHandler() {
        multiSelectActionHandler = MultiSelectActionHandler(
            context = this,
            lifecycleOwner = this,
            mainActivityViewModel = mainActivityViewModel,
            folderActivityViewModel = folderActivityViewModel,
            fragmentManager = supportFragmentManager,
            layoutInflater = layoutInflater,
            exitMultiSelectMode = { exitMultiSelectMode() },
            filterVisibleFolders = { it.visibleFolders(currentFolderId) },
            refreshRootFoldersUI = { updatedSubfolders ->
                subfolderAdapter.submitList(updatedSubfolders)
                latestSubfolders = updatedSubfolders
                updateSectionVisibility()
            }
        )
    }

    private fun showSelectionMenuPopup(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_multiselect, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_move -> MultiSelectHelper.handleMove(latestNotes, latestSubfolders, selectedNoteIds, selectedFolderIds, multiSelectActionHandler)
                    R.id.action_copy -> MultiSelectHelper.handleCopy(latestNotes, latestSubfolders, selectedNoteIds, selectedFolderIds, multiSelectActionHandler)
                    R.id.action_delete -> MultiSelectHelper.handleDelete(latestNotes, latestSubfolders, selectedNoteIds, selectedFolderIds, multiSelectActionHandler)
                    R.id.action_select_all -> SelectionUtils.handleSelectAll(latestNotes, latestSubfolders, selectedNoteIds, selectedFolderIds, noteAdapter, subfolderAdapter, ::updateSelectionCount)
                }
                true
            }
        }.show()
    }

    private fun setupToolbar() {
        with(binding.topBar) {
            tvTopBarTitle.text = originalFolderName
            tvTopBarTitle.onClick { setupRenameFolder() }
            ivNavToggle.setImageResource(R.drawable.ic_arrow_back)
            ivNavToggle.onClick { handleFolderRenameIfNeeded()
                onBackPressedDispatcher.onBackPressed() }
        }

        ToolbarHelper.setup(
            activity = this,
            toolbarBinding = binding.topBar,
            viewModel = mainActivityViewModel,
            title = originalFolderName,
            onClickNewFolder = {
                showNewFolderDialog(this, currentFolderId) { newFolder ->
                    folderActivityViewModel.insert(newFolder)
                }
            },
            onClickSort = { showSortBottomSheet() }
        )
    }

    private fun setupRenameFolder() {
        Helper.setupRenameFolder(
            binding = binding.topBar,
            originalFolderName = originalFolderName,
            currentFolderId = currentFolderId,
            folderActivityViewModel = folderActivityViewModel
        ) { newName ->
            originalFolderName = newName
            editedFolderName = newName
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
            folderActivityViewModel.renameFolder(currentFolderId, newName)
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
                if (isMultiSelectMode)
                    SelectionUtils.toggleNoteSelection(note, selectedNoteIds, noteAdapter, ::updateSelectionCount)
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
                    mainActivityViewModel = mainActivityViewModel,
                    folderActivityViewModel = folderActivityViewModel
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
                    folderActivityViewModel = folderActivityViewModel,
                    currentFolderId = currentFolderId
                )
            ).attachToRecyclerView(this)
        }
    }

    private fun enableMultiSelectMode() {
        if (isMultiSelectMode) return
        isMultiSelectMode = true
        MultiSelectHelper.enableMultiSelect(
            binding,
            noteAdapter,
            subfolderAdapter,
            selectedNoteIds,
            selectedFolderIds,
            ::updateSelectionCount
        )
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        MultiSelectHelper.exitMultiSelect(
            binding,
            noteAdapter,
            subfolderAdapter,
            selectedNoteIds,
            selectedFolderIds,
            ::updateSelectionCount
        )
    }

    private fun toggleNoteSelection(note: NoteEntity) {
        SelectionUtils.toggleNoteSelection(note, selectedNoteIds, noteAdapter, ::updateSelectionCount)
    }

    private fun toggleFolderSelection(folder: FolderEntity) {
        SelectionUtils.toggleFolderSelection(folder, selectedFolderIds, subfolderAdapter, ::updateSelectionCount)
    }

    private fun updateSelectionCount() {
        val count = selectedNoteIds.size + selectedFolderIds.size
        binding.tvSelectedCount.text = "$count Selected"
    }

    override fun onResume() {
        super.onResume()
        folderActivityViewModel.refreshSubfolders(currentFolderId)
    }

    private fun observeFolders() {
        folderActivityViewModel.getSubfolders(currentFolderId)
        folderActivityViewModel.subfolders.observe(this) { subfolders ->
            latestSubfolders = subfolders
            subfolderAdapter.submitList(subfolders)
            updateSectionVisibility()
        }
    }

    private fun observeNotes() {
        mainActivityViewModel.getNotesByFolderId(currentFolderId)
        mainActivityViewModel.folderNotes.observe(this) { notes ->
            latestNotes = notes
            noteAdapter.submitList(notes)
            updateSectionVisibility()
        }
    }

    private fun updateSectionVisibility() {
         updateSectionVisibility(
            binding = binding,
            hasSubfolders = latestSubfolders.isNotEmpty(),
            hasNotes = latestNotes.isNotEmpty()
        )
    }


    private fun observeViewMode() {
        mainActivityViewModel.viewMode.observe(this) { mode ->
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
            mainActivityViewModel.sortNotesInFolder(currentFolderId, sortBy, order)
            mainActivityViewModel.folderSortedNotes.observe(this) { sortedNotes ->
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