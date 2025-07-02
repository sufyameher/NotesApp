package com.example.notesapp

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.notesapp.folder.FolderAdapter
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.folder.FolderActionBottomSheet
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.common.Helper.toast
import com.example.notesapp.common.SortBottomSheet
import com.example.notesapp.databinding.ActivityMainBinding
import com.example.notesapp.folder.FolderEntity
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.folder.FolderActivity
import com.example.notesapp.util.ToolbarHelper
import com.example.notesapp.util.getLayoutManager
import com.example.notesapp.util.showMoveConfirmDialog
import com.example.notesapp.util.showNewFolderDialog
import com.example.notesapp.util.showNoteActions
import com.example.notesapp.folder.FolderViewModel
import com.example.notesapp.note.AddNoteActivity
import com.example.notesapp.note.NoteViewModel
import com.example.notesapp.trash.TrashActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: NoteViewModel by viewModels()
    private lateinit var folderViewModel: FolderViewModel

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var folderAdapter: FolderAdapter

    private var latestRootFolders: List<FolderEntity> = emptyList()
    private var latestRootNotes: List<NoteEntity> = emptyList()

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
                    startActivity(Intent(this@MainActivity, TrashActivity::class.java))
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
    }

    private fun setupAdapters() {
        noteAdapter = NoteAdapter(
            onItemClick = { note -> openNoteEditor(note.id) },
            onNoteLongClick = { note -> showNoteActions(
                context = this,
                fragmentManager = supportFragmentManager,
                note = note,
                getFolders = { callback ->
                    folderViewModel.activeFolders.value?.let { callback(it) }
                },
                onNoteUpdated = { viewModel.update(it) },
                onNoteInserted = { viewModel.insert(it) }
            )
            }
        )

        folderAdapter = FolderAdapter(
            folders = emptyList(),
            onFolderClick = { openFolder(it.id, it.name) },
            onFolderLongClick = { folder ->
                folderViewModel.getFolderInfo(folder.id).observeOnce(this) { info ->
                    showFolderActions(folder, info)
                }
            }
        )
    }

    private fun setupRecyclerViews() {
        binding.recyclerView.apply {
            adapter = noteAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }

        binding.folderRecyclerView.apply {
            adapter = folderAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
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


    private fun openNoteEditor(noteId: Int) {
        startActivity(Intent(this, AddNoteActivity::class.java).apply {
            putExtra("note_id", noteId)
        })
    }

    private fun openFolder(folderId: Int, folderName: String) {
        startActivity(Intent(this, FolderActivity::class.java).apply {
            putExtra("folder_id", folderId)
            putExtra("folder_name", folderName)
        })
    }

    private fun showFolderActions(folder: FolderEntity, folderInfo: String?) {
        FolderActionBottomSheet(
            folder = folder,
            folderInfo = folderInfo,
            onMoveTo = { handleMoveFolder(it) },
            onCopyTo = { handleCopyFolder(it) },
            onDelete = { confirmDeleteFolder(it) }
        ).show(supportFragmentManager, "FolderActionBottomSheet")
    }


    private fun handleMoveFolder(folderToMove: FolderEntity) {
        folderViewModel.activeFolders.value?.let { folders ->

            val availableFolders = folders

            if (availableFolders.isEmpty()) {
                toast("No folders available to move")
                return
            }

            showMoveConfirmDialog(
                context = this,
                inflater = layoutInflater,
                folders = availableFolders,
                titleText  = "Move to",
                currentFolderId = folderToMove.id
            ) { targetFolder ->
                targetFolder?.let {
                    folderViewModel.moveFolderToParent(folderToMove, it.id)
                    toast("Folder moved to '${it.name}'")
                }
            }
        }
    }

    private fun handleCopyFolder(selectedFolder: FolderEntity) {
        folderViewModel.allFolders.observeOnce(this) { allFolders ->
            val availableFolders = allFolders.filter { it.id != selectedFolder.id }

            if (availableFolders.isEmpty()) {
                toast("No folders available to copy to")
                return@observeOnce
            }

            val parentFolder = allFolders.find { it.id == selectedFolder.parentFolderId }

            showMoveConfirmDialog(
                context = this,
                inflater = layoutInflater,
                folders = availableFolders,
                currentFolderId = selectedFolder.id,
                startFromFolder = parentFolder,
                titleText = "Copy to"
            ) { targetFolder ->
                if (targetFolder != null) {
                    folderViewModel.copyFolderWithContents(selectedFolder, targetFolder.id) {
                        folderViewModel.refreshSubfolders(targetFolder.id, onlyIfVisible = 0)
                        toast("Folder copied to '${targetFolder.name}'")
                    }
                }
            }
        }
    }


    private fun confirmDeleteFolder(folderToDelete: FolderEntity) {
        AlertDialog.Builder(this)
            .setTitle("Are you sure?")
            .setMessage("This folder and its notes will be moved to Recently Deleted.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                folderViewModel.deleteFolderAndNotes(folderToDelete)
                toast("Folder deleted successfully")
            }.show()
    }
}
