package com.example.notesapp.note

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.notesapp.common.bottomsheet.gallery.GalleryBottomSheet
import com.example.notesapp.JSBridge
import com.example.notesapp.common.Helper.hideKeyboard
import com.example.notesapp.common.Helper.requestStoragePermissionIfNeeded
import com.example.notesapp.common.onClick
import com.example.notesapp.databinding.ActivityAddNoteBinding
import dagger.hilt.android.AndroidEntryPoint
import org.json.JSONObject
import timber.log.Timber


@AndroidEntryPoint
class AddNoteActivity : AppCompatActivity() {

    private val binding by lazy { ActivityAddNoteBinding.inflate(layoutInflater) }
    private val mainActivityViewModel: MainActivityViewModel by viewModels()
    private var noteId: Int? = null
    private var existingNote: NoteEntity? = null
    private var folderId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        initIntentData()
        setupWebView()
        setupView()
        setupButtons()
        loadExistingNoteIfNeeded()
        requestStoragePermissionIfNeeded(this)
    }

    private fun initIntentData() {
        noteId = intent.getIntExtra("note_id", -1).takeIf { it != -1 }
        folderId = intent.getIntExtra("folder_id", 0).takeIf { it != -1 }!!
    }

    private fun setupWebView() = with(binding.webView) {
        settings.javaScriptEnabled = true
        addJavascriptInterface(JSBridge(this@AddNoteActivity), "Android")
        webViewClient = WebViewClient()
        loadUrl("file:///android_asset/checklist_editor.html")
    }

    private fun setupView() {
        binding.etTitle.requestFocus()
        binding.ivBack.onClick { handleNoteSaveAndExit() }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val byteArray = inputStream?.readBytes() ?: return ""
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    private fun setupButtons() = with(binding) {
        btnCheckbox.onClick { runJS("insertCheckbox();") }
        btnBullet.onClick { runJS("insertBulletPoint();") }
        btnNumber.onClick { runJS("insertNumberedItem();") }
        btnImage.onClick { handleImageInsert() }
    }

    private fun runJS(script: String) {
        binding.webView.evaluateJavascript(script, null)
    }

    private fun handleImageInsert() {
        GalleryBottomSheet.show(this) { uris ->
            uris.forEach { uri ->
                val base64 = encodeImageToBase64(uri)
                runJS("insertImageFromBase64('data:image/png;base64,$base64');")
            }
        }
    }

    private fun loadExistingNoteIfNeeded() {
        noteId?.let { id ->
            mainActivityViewModel.getNoteById(id)
            mainActivityViewModel.noteById.observe(this) { note ->
                note?.let {
                    existingNote = it
                    binding.etTitle.setText(it.title)
                    Timber.d("Loaded Note Desc: ${it.description}")
                    it.description?.let { it1 -> waitForWebViewAndLoad(it1) }
                }
            }
        }
    }

    fun waitForWebViewAndLoad(desc: String, retries: Int = 10) {
        if (retries <= 0) return

        binding.webView.evaluateJavascript("window.webviewReady === true") { result ->
            if (result == "true") {
                val safeJSString = JSONObject.quote(desc)
                binding.webView.evaluateJavascript("loadChecklistFromDescription($safeJSString);", null)
            } else {
                binding.webView.postDelayed({
                    waitForWebViewAndLoad(desc, retries - 1)
                }, 100)
            }
        }
    }

    override fun onBackPressed() {
        handleNoteSaveAndExit()
    }

    private fun handleNoteSaveAndExit() {
        binding.root.hideKeyboard()
        val title = binding.etTitle.text.toString().trim()

        binding.webView.evaluateJavascript("getChecklistData();") { result ->
            val desc = result.removeSurrounding("\"").replace("\\n", "\n").trim()
            val isTitleEmpty = title.isBlank()
            val isDescEmpty = desc.isBlank()

            if (!isTitleEmpty || !isDescEmpty) {
                mainActivityViewModel.saveOrUpdateNote(title, desc, folderId, existingNote)
            } else if (existingNote == null) {
                Timber.d("New empty note — not saving")
            } else {
                Timber.d("Existing note left empty — skipping save")
            }
            onBackPressedDispatcher.onBackPressed()
        }
    }

    companion object {
        fun launch(
            context: Context,
            noteId: Int? = null,
            folderId: Int? = null
        ) {
            val intent = Intent(context, AddNoteActivity::class.java).apply {
                noteId?.let { putExtra("note_id", it) }
                folderId?.let { putExtra("folder_id", it) }
            }
            context.startActivity(intent)
        }
    }
}
