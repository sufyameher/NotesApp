package com.example.notesapp.note

import android.content.Context
import android.os.Bundle
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.databinding.ActivityAddNoteBinding

class AddNoteActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAddNoteBinding.inflate(layoutInflater) }
    private val viewModel: NoteViewModel by viewModels()
    private var noteId: Int? = null
    private var existingNote: NoteEntity? = null
    private var folderId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        initIntentData()
        setupView()
        loadExistingNoteIfNeeded()
    }

    private fun initIntentData() {
        noteId = intent.getIntExtra("note_id", -1).takeIf { it != -1 }
        folderId = intent.getIntExtra("folder_id", -1).takeIf { it != -1 }

    }

    private fun setupView() {
        binding.etTitle.requestFocus()
        binding.ivBack.setOnClickListener {
            handleNoteSaveAndExit()
        }
    }

    private fun loadExistingNoteIfNeeded() {
        noteId?.let { id ->
            viewModel.getNoteById(id).observe(this) { note ->
                note?.let {
                    existingNote = it
                    binding.etTitle.setText(it.title)
                    binding.etDesc.setText(it.description)
                }
            }
        }
    }

    override fun onBackPressed() {
        handleNoteSaveAndExit()
        super.onBackPressed()
    }

    private fun handleNoteSaveAndExit() {
        hideKeyboard()

        val title = binding.etTitle.text.toString().trim()
        val desc = binding.etDesc.text.toString().trim()

        if (title.isNotEmpty() || desc.isNotEmpty()) {
            saveNote(title, desc)
        }
        finish()
    }

    private fun saveNote(title: String, desc: String) {
        val now = System.currentTimeMillis()
        val noteTitle = if (desc.isNotEmpty()) "$title." else title

        val note = existingNote?.copy(
            title = noteTitle,
            description = desc,
            modifiedDate = now
        ) ?: NoteEntity(
            title = title,
            description = desc,
            createdDate = now,
            modifiedDate = now,
            folderId = folderId
        )

        if (existingNote != null) viewModel.update(note) else viewModel.insert(note)
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
    }
}
