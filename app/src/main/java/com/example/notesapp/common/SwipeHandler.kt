package com.example.notesapp.common

import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.notesapp.common.Helper.observeOnce
import com.example.notesapp.folder.adapter.FolderAdapter
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.folder.data.FolderActivityViewModel
import com.example.notesapp.note.NoteAdapter
import com.example.notesapp.note.NoteEntity
import com.example.notesapp.note.MainActivityViewModel
import com.example.notesapp.util.handleFolderSwipe
import com.example.notesapp.util.handleNoteSwipe
import com.example.notesapp.util.showFolderActionSheet
import com.example.notesapp.util.showNoteActions
import kotlin.collections.getOrNull


fun getNoteSwipeCallback(
    activity: FragmentActivity,
    noteAdapter: NoteAdapter,
    folderActivityViewModel: FolderActivityViewModel,
    viewModel: MainActivityViewModel
    ): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val note = noteAdapter.currentList.getOrNull(position) ?: return

                handleNoteSwipe(
                    activity = activity,
                    note = note,
                    position = position,
                    noteAdapter = noteAdapter,
                    folderActivityViewModel = folderActivityViewModel,
                    viewModel = viewModel
                )
            }
        }
    }

    fun getFolderSwipeCallback(
        activity: FragmentActivity,
        latestRootFolders: () -> List<FolderEntity>,
        folderAdapter: FolderAdapter,
        folderActivityViewModel: FolderActivityViewModel
    ): ItemTouchHelper.SimpleCallback {
        return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val folder = latestRootFolders().getOrNull(position) ?: return

                handleFolderSwipe(
                    activity = activity,
                    folder = folder,
                    position = position,
                    folderAdapter = folderAdapter,
                    folderActivityViewModel = folderActivityViewModel
                )
            }
        }
    }

fun getFolderActivityNoteSwipeCallback(
    activity: FragmentActivity,
    latestNotes: () -> List<NoteEntity>,
    noteAdapter: NoteAdapter,
    mainActivityViewModel: MainActivityViewModel,
    folderActivityViewModel: FolderActivityViewModel
): ItemTouchHelper.SimpleCallback {
    return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val note = latestNotes().getOrNull(position)

            if (note != null) {
                showNoteActions(
                    context = activity,
                    fragmentManager = activity.supportFragmentManager,
                    note = note,
                    getFolders = { callback ->
                        folderActivityViewModel.allFolders.observe(activity) { callback(it) }
                    },
                    onNoteUpdated = { mainActivityViewModel.update(it) },
                    onNoteInserted = { mainActivityViewModel.insert(it) },
                    onTogglePin = {
                         mainActivityViewModel.togglePin(it)
                    }
                )
                noteAdapter.notifyItemChanged(position)
            } else {
                noteAdapter.notifyItemChanged(position)
            }
        }
    }
}

fun getFolderActivitySubfolderSwipeCallback(
    activity: FragmentActivity,
    latestSubfolders: () -> List<FolderEntity>,
    folderAdapter: FolderAdapter,
    folderActivityViewModel: FolderActivityViewModel,
    currentFolderId: Int
): ItemTouchHelper.SimpleCallback {
    return object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val folder = latestSubfolders().getOrNull(position)

            if (folder != null) {
                folderActivityViewModel.getFolderInfo(folder.id)
                folderActivityViewModel.folderInfo.observeOnce(activity) { info ->
                    showFolderActionSheet(
                        activity = activity,
                        folder = folder,
                        folderInfo = info,
                        folderActivityViewModel = folderActivityViewModel,
                        currentFolderId = currentFolderId
                    )
                    folderAdapter.notifyItemChanged(position)
                }
            } else {
                folderAdapter.notifyItemChanged(position)
            }
        }
    }
}

fun getNoteSwipeForTrash(
    adapter: NoteAdapter,
    onSwipedNote: (NoteEntity) -> Unit
): ItemTouchHelper {
    val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val note = adapter.currentList.getOrNull(position)

            if (note != null) {
                onSwipedNote(note)
            }

            adapter.notifyItemChanged(position)
        }
    }

    return ItemTouchHelper(callback)
}

// TODO: make 1 reusable fun

fun <T> SwipeCallback(        
    getItemAt: (position: Int) -> T?,
    onSwiped: (item: T, position: Int) -> Unit,   
    onItemNotFound: ((position: Int) -> Unit)? = null,
    afterHandled: ((position: Int) -> Unit)? = null
): ItemTouchHelper {
    val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ) = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val item = getItemAt(position)

            if (item != null) {
                onSwiped(item, position)
            } else {
                onItemNotFound?.invoke(position)
            }

            afterHandled?.invoke(position)
        }
    }

    return ItemTouchHelper(callback)
}

