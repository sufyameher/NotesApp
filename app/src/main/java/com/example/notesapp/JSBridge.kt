package com.example.notesapp

import android.webkit.JavascriptInterface
import android.widget.Toast
import com.example.notesapp.note.AddNoteActivity

class JSBridge(private val activity: AddNoteActivity) {

    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
}
