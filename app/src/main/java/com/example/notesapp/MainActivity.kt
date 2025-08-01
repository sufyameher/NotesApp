package com.example.notesapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
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
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.common.multiselect.MultiSelectActionHandler
import com.example.notesapp.common.bottomsheet.SortBottomSheet
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.folder.data.FolderViewModel
import com.example.notesapp.folder.ui.FolderActionBottomSheet
import com.example.notesapp.folder.ui.FolderActivity
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.NoteViewModel
import com.example.notesapp.trash.TrashActivity
import com.example.notesapp.common.ToolbarHelper
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showMultiFolderDialog
import com.example.notesapp.util.showNewFolderDialog
import com.example.notesapp.util.showNoteActions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var folderViewModel: FolderViewModel

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
        setupAdapters()
        setupRecyclerViews()
        setupObservers()
        setupFab()
        setupToolbar()
        setupDrawer()


        binding.ivCloseSelection.setOnClickListener {
            exitMultiSelectMode()
        }

        binding.ivSelectionMenu.setOnClickListener {
            showSelectionMenuPopup(it)
        }


//        binding.ivFolderArrow.setOnClickListener {
//            binding.folderRecyclerView.isVisible = !binding.folderRecyclerView.isVisible
//            // Optionally animate arrow rotation
//        }
    }

    private fun setupToolbar() {
        ToolbarHelper.setup(
            activity = this,
            toolbarBinding = binding.topBar,
            viewModel = viewModel,
            title = getString(R.string.app_name),
            onClickNewFolder = {
                showNewFolderDialog(context = this, parentFolderId = null) { folderViewModel.insert(it) }
            },
            onClickSort = { showSortBottomSheet() }
        )
    }

    private fun setupDrawer() = with(binding) {
        topBar.ivNavToggle.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            drawerLayout.closeDrawer(GravityCompat.START)
            when (menuItem.itemId) {
                R.id.nav_home -> true
                R.id.nav_recently_deleted -> {
                    TrashActivity.start(this@MainActivity)
                    true
                }
                else -> false
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }


    private fun initViewModels() {
        folderViewModel = ViewModelProvider(this)[FolderViewModel::class.java]
        multiSelectHandler = MultiSelectActionHandler(
            context = this,
            noteViewModel = viewModel,
            folderViewModel = folderViewModel,
            fragmentManager = supportFragmentManager,
            layoutInflater = layoutInflater,
            lifecycleOwner = this,
            exitMultiSelectMode = { exitMultiSelectMode() }
        )
    }

    private fun setupAdapters() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> openNoteEditor(note.id) },
            onNoteLongClick = { enableMultiSelectMode() }
        ).apply {
            onSelectionToggle = { note ->
                toggleNoteSelection(note)
            }
        }

        folderAdapter = FolderAdapter(
            folders = emptyList(),
            onFolderClick = { openFolder(it.id, it.name) },
            onFolderLongClick = { enableMultiSelectMode() }
        ).apply {
            onSelectionToggle = { folder ->
                toggleFolderSelection(folder)
            }
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
            binding.topBar.root.visibility = View.GONE
            binding.selectionTopBar.visibility = View.VISIBLE
            binding.tvSelectedCount.text = "$totalSelected Selected"
        } else {
            exitMultiSelectMode()
        }
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
        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val note = noteAdapter.currentList[position]

                    showNoteActions(
                        context = this@MainActivity,
                        fragmentManager = supportFragmentManager,
                        note = note,
                        getFolders = { callback ->
                            folderViewModel.activeFolders.value?.let { callback(it) }
                        },
                        onNoteUpdated = { viewModel.update(it) },
                        onNoteInserted = { viewModel.insert(it) }
                    )

                    noteAdapter.notifyItemChanged(position)
                }
            }

            ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.recyclerView)
        }

        binding.folderRecyclerView.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)

            val folderSwipeCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean = false

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val position = viewHolder.adapterPosition
                    val folder = latestRootFolders.getOrNull(position) ?: return

                    folderViewModel.getFolderInfo(folder.id).observeOnce(this@MainActivity) { info ->
                        showFolderActions(folder, info)
                        folderAdapter.notifyItemChanged(position)
                    }
                }
            }

            ItemTouchHelper(folderSwipeCallback).attachToRecyclerView(this)
        }
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            startActivity(Intent(this, AddNoteActivity::class.java))
        }
    }

    private fun showSortBottomSheet() {
        SortBottomSheet { sortBy, order -> viewModel.sortNotes(sortBy, order)
        }.show(supportFragmentManager, "SortBottomSheet")
    }

    private fun updateMainVisibility() {
        binding.apply {
            val hasFolders = latestRootFolders.isNotEmpty()
            val hasNotes = latestRootNotes.isNotEmpty()

            tvFolders.visibility = if (hasFolders) View.VISIBLE else View.GONE
            tvNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE

            bottomSpacer.visibility = if (hasFolders) View.VISIBLE else View.GONE
            bottomSpacer1.visibility = if (hasNotes) View.VISIBLE else View.GONE
        }
    }

    private fun setupObservers() {
        viewModel.allNotes.observe(this) { notes ->
            val notesWithoutFolder = notes.filter { it.folderId == null && !it.isDeleted }
            latestRootNotes = notesWithoutFolder
            binding.apply {
                noteAdapter.submitList(notesWithoutFolder)
            }
            updateMainVisibility()

        }

        folderViewModel.activeFolders.observe(this) { folders ->
            val rootFolders = folders.filter { it.parentFolderId == null }
            val shouldShowFolders = rootFolders.isNotEmpty()

            latestRootFolders = rootFolders

            binding.apply {
                folderAdapter.updateFolders(rootFolders)
                tvFolders.visibility = if (shouldShowFolders) View.VISIBLE else View.GONE
                folderRecyclerView.visibility = if (shouldShowFolders) View.VISIBLE else View.GONE
            }
            updateMainVisibility()

        }

        viewModel.viewMode.observe(this) { mode ->
            binding.recyclerView.layoutManager = getLayoutManager(this, mode)
            binding.folderRecyclerView.layoutManager = getLayoutManager(this, mode)
        }
    }

    private fun showSelectionMenuPopup(anchor: View) {
        PopupMenu(this, anchor).apply {
            menuInflater.inflate(R.menu.menu_multiselect, menu)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_move -> {
                        handleMoveSelectedItemsAndFolders()
                    }
                    R.id.action_copy -> {
                        handleCopySelectedItemsAndFolders()
                    }
                    R.id.action_delete -> {
                        handleDeleteSelectedItemsAndFolders()
                    }
                    R.id.action_select_all -> {
                        handleSelectAllItemsAndFolders()
                    }
                }
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
        startActivity(Intent(this, AddNoteActivity::class.java).apply {
            putExtra("note_id", noteId)
        })
    }

    private fun openFolder(folderId: Int, folderName: String) {
        FolderActivity.start(this, folderId, folderName)
    }


    private fun showFolderActions(folder: FolderEntity, folderInfo: String?) {
        FolderActionBottomSheet(
            folder = folder,
            folderInfo = folderInfo,
            onMoveTo = { handleMoveSelectedFolders() },
            onCopyTo = { handleCopySelectedFolders() },
            onDelete = { handleDeleteSelectedFolders() }
        ).show(supportFragmentManager, "FolderActionBottomSheet")
    }


    private fun handleMoveSelectedFolders() {
        val selectedFolders = latestRootFolders.filter { folderAdapter.selectedIds.contains(it.id) }

        if (selectedFolders.isEmpty()) return

        folderViewModel.activeFolders.value?.let { allFolders ->

            val availableFolders = allFolders

            if (availableFolders.isEmpty()) {
                toast("No folders available to move")
                return
            }

            val excludedIds = selectedFolders.map { it.id }.toSet()

            showMultiFolderDialog(
                context = this,
                inflater = layoutInflater,
                folders = availableFolders,
                excludedFolderIds = excludedIds,
                titleText = "Move to",
                startFromFolder = null
            ) { targetFolder ->
                targetFolder?.let {
                    selectedFolders.forEach { folder ->
                        folderViewModel.moveFolderToParent(folder, it.id)
                    }
                    toast("Moved ${selectedFolders.size} folder(s) to '${it.name}'")
                    exitMultiSelectMode()
                }
            }
        }
    }

    private fun handleCopySelectedFolders() {
        val selectedFolders = latestRootFolders.filter { folderAdapter.selectedIds.contains(it.id) }

        if (selectedFolders.isEmpty()) return

        folderViewModel.allFolders.observeOnce(this) { allFolders ->

            val availableFolders = allFolders
            if (availableFolders.isEmpty()) {
                toast("No folders available to copy to")
                return@observeOnce
            }

            val excludedIds = selectedFolders.map { it.id }.toSet()

            showMultiFolderDialog(
                context = this,
                inflater = layoutInflater,
                folders = availableFolders,
                excludedFolderIds = excludedIds,
                titleText = "Copy to",
                startFromFolder = null
            ) { targetFolder ->
                targetFolder?.let {
                    selectedFolders.forEach { folder ->
                        folderViewModel.copyFolderWithContents(folder, it.id) {
                            folderViewModel.refreshSubfolders(it.id, onlyIfVisible = 0)
                        }
                    }
                    toast("Copied ${selectedFolders.size} folder(s) to '${it.name}'")
                    exitMultiSelectMode()
                }
            }
        }
    }

    private fun handleDeleteSelectedFolders() {
        val selectedFolders = latestRootFolders.filter { folderAdapter.selectedIds.contains(it.id) }

        if (selectedFolders.isEmpty()) return

        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("These ${selectedFolders.size} folder(s) and their notes will be moved to Recently Deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                selectedFolders.forEach { folder ->
                    folderViewModel.deleteFolderAndNotes(folder)
                }
                toast("Deleted ${selectedFolders.size} folder(s)")
                exitMultiSelectMode()
            }.show()
    }

    override fun onBackPressed() {
        if (isMultiSelectMode) {
            exitMultiSelectMode()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        fun start(context: Context) {

        }
    }
}