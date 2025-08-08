package com.example.notesapp

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.PopupMenu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.common.Helper
import com.example.notesapp.common.PreferenceUtil
import com.example.notesapp.common.ToolbarHelper
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.common.getFolderSwipeCallback
import com.example.notesapp.common.getNoteSwipeCallback
import com.example.notesapp.common.gone
import com.example.notesapp.common.multiselect.MultiSelectActionHandler
import com.example.notesapp.common.onClick
import com.example.notesapp.common.setVisibleIf
import com.example.notesapp.common.show
import com.example.notesapp.common.visibleFolders
import com.example.notesapp.common.visibleIf
import com.example.notesapp.common.visibleIfNotEmpty
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.folder.ui.FolderActivity
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.MainActivityViewModel
import com.example.notesapp.trash.TrashActivity
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showNewFolderDialog
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private lateinit var folderActivityViewModel: FolderActivityViewModel

    private lateinit var multiSelectHandler: MultiSelectActionHandler

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter

    private var latestRootFolders: List<FolderEntity> = emptyList()
    private var latestRootNotes: List<NoteEntity> = emptyList()

    private var isMultiSelectMode = false
    private val selectedNoteIds = mutableSetOf<Int>()
    private val selectedFolderIds = mutableSetOf<Int>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViewModels()
        //viewModel.sortNotes(viewModel.rootFolderId, PreferenceUtil.sortBy.name, PreferenceUtil.sortOrder.name)

        setupAdapters()
        setupRecyclerViews()
        setupObservers()
        setupFab()
        setupToolbar()
        setupDrawer()

        binding.ivCloseSelection.setOnClickListener { exitMultiSelectMode() }
        binding.ivSelectionMenu.setOnClickListener { showSelectionMenuPopup(it) }

        /*binding.ivFolderArrow.setOnClickListener {
            binding.folderRecyclerView.isVisible = !binding.folderRecyclerView.isVisible
            //Optionally animate arrow rotation
        }*/
    }

    private fun initViewModels() {
        folderActivityViewModel = ViewModelProvider(this)[FolderActivityViewModel::class.java]
        folderActivityViewModel.loadInitialFolders()

        multiSelectHandler = MultiSelectActionHandler(
            context = this,
            mainActivityViewModel = viewModel,
            folderActivityViewModel = folderActivityViewModel,
            fragmentManager = supportFragmentManager,
            layoutInflater = layoutInflater,
            lifecycleOwner = this,
            exitMultiSelectMode = { exitMultiSelectMode() },
            filterVisibleFolders = { it.visibleFolders(null) } ,
            refreshRootFoldersUI = { updatedFolders ->
                folderAdapter.submitList(updatedFolders)
                latestRootFolders = updatedFolders
                updateMainVisibility()
            }
        )
    }

    private fun setupToolbar() {
        ToolbarHelper.setup(
            activity = this,
            toolbarBinding = binding.topBar,
            viewModel = viewModel,
            title = getString(R.string.app_name),
            onClickNewFolder = {
                showNewFolderDialog(context = this, parentFolderId = null) { newFolder ->
                    folderActivityViewModel.insert(newFolder)
                    latestRootFolders = latestRootFolders + newFolder
                    folderAdapter.submitList(latestRootFolders)
                    updateMainVisibility()
                }
            },
            onClickSort = { showSortBottomSheet() }
        )
    }

    private fun setupDrawer() = with(binding) {
        topBar.ivNavToggle.onClick { drawerLayout.openDrawer(GravityCompat.START) }
        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_recently_deleted -> { TrashActivity.start(this@MainActivity); true }
                else -> false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun setupAdapters() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> openNoteEditor(note.id) },
            onNoteLongClick = { enableMultiSelectMode() }
        ).apply {
            onSelectionToggle = ::toggleNoteSelection
        }

        folderAdapter = FolderAdapter(
            onFolderClick = { openFolder(it.id, it.name) },
            onFolderLongClick = { enableMultiSelectMode() }
        ).apply {
            onSelectionToggle = { ::toggleFolderSelection}
        }
    }

    private fun toggleNoteSelection(note: NoteEntity) {
        if (selectedNoteIds.contains(note.id)) selectedNoteIds.remove(note.id)
        else selectedNoteIds.add(note.id)
        updateSelectionUI()
        noteAdapter.selectedIds = selectedNoteIds
        noteAdapter.notifyDataSetChanged()
    }

    private fun toggleFolderSelection(folder: FolderEntity) {
        if (selectedFolderIds.contains(folder.id)) selectedFolderIds.remove(folder.id)
        else selectedFolderIds.add(folder.id)
        updateSelectionUI()
        folderAdapter.selectedIds = selectedFolderIds
        folderAdapter.notifyDataSetChanged()
    }

    private fun updateSelectionUI() {
        val totalSelected = selectedNoteIds.size + selectedFolderIds.size
        if (totalSelected > 0) {
            binding.topBar.root.gone()
            binding.selectionTopBar.show()
            binding.tvSelectedCount.text = "$totalSelected Selected"
        } else {
            exitMultiSelectMode()
        }
    }

    private fun enableMultiSelectMode() {
        if (isMultiSelectMode) return
        isMultiSelectMode = true
        noteAdapter.isMultiSelectMode = true
        folderAdapter.isMultiSelectMode = true
        noteAdapter.notifyDataSetChanged()
        folderAdapter.notifyDataSetChanged()
        updateSelectionUI()
    }

    private fun exitMultiSelectMode() {
        isMultiSelectMode = false
        selectedNoteIds.clear()
        selectedFolderIds.clear()
        noteAdapter.isMultiSelectMode = false
        folderAdapter.isMultiSelectMode = false
        noteAdapter.selectedIds = emptySet()
        folderAdapter.selectedIds = emptySet()
        noteAdapter.notifyDataSetChanged()
        folderAdapter.notifyDataSetChanged()
        binding.topBar.root.visibility = View.VISIBLE
        binding.selectionTopBar.visibility = View.GONE
    }

    private fun setupRecyclerViews() {
        setupNoteRecyclerView()
        setupFolderRecyclerView()
    }

    private fun setupNoteRecyclerView() {
        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            ItemTouchHelper(
                getNoteSwipeCallback(
                    activity = this@MainActivity,
                    noteAdapter = noteAdapter,
                    folderActivityViewModel = folderActivityViewModel,
                    viewModel = viewModel
                )
            ).attachToRecyclerView(this)
        }
    }

    private fun setupFolderRecyclerView() {
        binding.folderRecyclerView.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
            ItemTouchHelper(
                getFolderSwipeCallback(
                    activity = this@MainActivity,
                    latestRootFolders = { latestRootFolders },
                    folderAdapter = folderAdapter,
                    folderActivityViewModel = folderActivityViewModel
                )
            ).attachToRecyclerView(this)
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener { AddNoteActivity.launch(this) }
    }

    private fun showSortBottomSheet() {
        SortBottomSheet.show(supportFragmentManager) { sortBy, order ->
            viewModel.sortNotes(viewModel.rootFolderId, PreferenceUtil.sortBy.name, PreferenceUtil.sortOrder.name)
        }
    }

    private fun updateMainVisibility() {
        binding.apply {
            tvFolders.visibleIfNotEmpty(latestRootFolders)
            bottomSpacer.visibleIfNotEmpty(latestRootFolders)
            tvNotes.visibleIfNotEmpty(latestRootNotes)
            bottomSpacer1.visibleIfNotEmpty(latestRootNotes)
        }
    }

    private fun setupObservers() {
        lifecycleScope.launchWhenStarted {
            viewModel.activeNotesFlow.collect { rootNotes ->
                Timber.d("Root notes received in UI: ${rootNotes.map { it.title }}")

                latestRootNotes = rootNotes
                latestRootNotes.forEachIndexed { index, note ->
                    Timber.d("[$index] ${note.title} | pinned=${note.isPinned} | created=${note.createdDate} | modified=${note.modifiedDate}")
                }
                noteAdapter.submitList(latestRootNotes)
                updateMainVisibility()
            }
        }

        folderActivityViewModel.activeFolders.observe(this) { folders ->
            val rootFolders = folders.filter { it.parentFolderId == null }
            val shouldShowFolders = rootFolders.isNotEmpty()
            latestRootFolders = rootFolders

            binding.apply {
                folderAdapter.submitList(rootFolders)
                tvFolders.setVisibleIf(shouldShowFolders)
                folderRecyclerView.setVisibleIf(shouldShowFolders) }
            updateMainVisibility()
        }

        viewModel.viewMode.observe(this) { mode ->
            binding.recyclerView.layoutManager = getLayoutManager(this, mode)
            binding.folderRecyclerView.layoutManager = getLayoutManager(this, mode)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.sortNotes(folderId = 0, sortBy = "DATE_CREATED", order = "DESC")
    }


    private fun showSelectionMenuPopup(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_multiselect, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_move -> { handleMoveSelectedItemsAndFolders() }
                    R.id.action_copy -> { handleCopySelectedItemsAndFolders() }
                    R.id.action_delete -> { handleDeleteSelectedItemsAndFolders() }
                    R.id.action_select_all -> { handleSelectAllItemsAndFolders() } }
                true
            }
        }.show()
    }

    private fun handleMoveSelectedItemsAndFolders() {
        val selectedNotes = noteAdapter.currentList.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestRootFolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectHandler.handleMove(selectedNotes, selectedFolders)
    }

    private fun handleCopySelectedItemsAndFolders() {
        val selectedNotes = noteAdapter.currentList.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestRootFolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectHandler.handleCopy(selectedNotes, selectedFolders)
    }

    private fun handleDeleteSelectedItemsAndFolders() {
        val selectedNotes = noteAdapter.currentList.filter { selectedNoteIds.contains(it.id) }
        val selectedFolders = latestRootFolders.filter { selectedFolderIds.contains(it.id) }
        multiSelectHandler.handleDelete(selectedNotes, selectedFolders)
    }

    private fun handleSelectAllItemsAndFolders() {
        val allNoteIds = latestRootNotes.map { it.id }
        val allFolderIds = latestRootFolders.map { it.id }

        val allAlreadySelected = selectedNoteIds.containsAll(allNoteIds) && selectedFolderIds.containsAll(allFolderIds)

        if (allAlreadySelected) {
            exitMultiSelectMode()
        } else {
            enableMultiSelectMode()
            selectedNoteIds.clear()
            selectedFolderIds.clear()
            selectedNoteIds.addAll(allNoteIds)
            selectedFolderIds.addAll(allFolderIds)
            noteAdapter.selectedIds = selectedNoteIds
            folderAdapter.selectedIds = selectedFolderIds
            noteAdapter.notifyDataSetChanged()
            folderAdapter.notifyDataSetChanged()
            updateSelectionUI()
        }
    }

    private fun openNoteEditor(noteId: Int) {
        AddNoteActivity.launch(this, noteId = noteId)
    }

    private fun openFolder(folderId: Int, folderName: String) {
        FolderActivity.start(this, folderId, folderName)
    }
}