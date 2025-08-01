package com.example.notesapp.common

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.notesapp.databinding.FolderActivityBinding
import com.example.notesapp.folder.data.FolderEntity
import com.example.notesapp.note.NoteEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Helper {
    fun View.updateSpacerVisibility(folders: List<FolderEntity>, notes: List<NoteEntity>) {
         visibility = if (folders.isNotEmpty() && notes.isNotEmpty()) View.VISIBLE else View.GONE
    }

    fun <T> LifecycleOwner.observeFlow(
        flow: Flow<T>,
        minActiveState: Lifecycle.State = Lifecycle.State.STARTED,
        onEach: (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(minActiveState) {
                flow.collect { onEach(it) }
            }
        }
    }

    fun updateSectionVisibility(
        binding: FolderActivityBinding,
        hasSubfolders: Boolean,
        hasNotes: Boolean
    ) {
        binding.apply {
            tvSubfolders.visibility = if (hasSubfolders) View.VISIBLE else View.GONE
            bottomSpacer.visibility = tvSubfolders.visibility
            tvNotes.visibility = if (hasNotes) View.VISIBLE else View.GONE
            bottomSpacer1.visibility = tvNotes.visibility
        }
    }

    fun Context.toast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, msg, duration).show()
    }

    fun Context.toastLong(msg: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(this, msg, duration).show()
    }

    fun View.showKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    inline fun <T> LiveData<T>.observeOnce(
        lifecycleOwner: LifecycleOwner,
        crossinline observer: (T) -> Unit
    ) {
        observe(lifecycleOwner) { value ->
            removeObservers(lifecycleOwner)
            observer(value)
        }
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun requestStoragePermissionIfNeeded(activity: Activity, requestCode: Int = 101) {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
        }
    }

    fun sortNotesList(
        notes: List<NoteEntity>,
        sortBy: String,
        order: String
    ): List<NoteEntity> {
        val comparator = compareBy<NoteEntity> { it.isPinned.not() }.thenBy {
            when (sortBy) {
                "TITLE" -> it.title?.lowercase() ?: ""
                "DATE_CREATED" -> it.createdDate
                "DATE_EDITED" -> it.modifiedDate
                else -> it.modifiedDate
            }
        }
        return if (order == "DESCENDING" || order == "Z - A") notes.sortedWith(comparator.reversed())
        else notes.sortedWith(comparator)
    }

}