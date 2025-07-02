package com.example.notesapp.common

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.lifecycle.LiveData
import com.example.notesapp.folder.FolderEntity
import com.example.notesapp.note.NoteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Helper {
    fun View.updateSpacerVisibility(folders: List<FolderEntity>, notes: List<NoteEntity>) {
        // ✅ Visible only if BOTH folders AND notes exist
        visibility = if (folders.isNotEmpty() && notes.isNotEmpty()) View.VISIBLE else View.GONE
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
        lifecycleOwner: androidx.lifecycle.LifecycleOwner,
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
}