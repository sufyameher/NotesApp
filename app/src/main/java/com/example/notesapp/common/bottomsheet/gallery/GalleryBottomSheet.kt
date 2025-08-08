package com.example.notesapp.common.bottomsheet.gallery

import android.content.Context
import android.net.Uri
import android.os.Bundle
import com.example.notesapp.common.Helper.resize
import com.example.notesapp.common.onClick
import com.example.notesapp.common.setupGrid
import com.example.notesapp.common.toggle
import com.example.notesapp.common.visibleIf
import com.example.notesapp.databinding.LayoutGalleryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialog

class GalleryBottomSheet(
    private val context: Context,
    private val onImagesSelected: (List<Uri>) -> Unit
) : BottomSheetDialog(context) {

    private val selectedUris = mutableSetOf<Uri>()
    private lateinit var binding: LayoutGalleryBottomSheetBinding
    private lateinit var adapter: ImageGalleryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutGalleryBottomSheetBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        loadImagesFromGallery()
        setupButton()
      }

    private fun setupButton() {
        binding.btnDone.onClick {
            onImagesSelected(selectedUris.toList())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        resize(0.75f)
    }

    private fun loadImagesFromGallery(){
        adapter.submitList(context.loadImagesFromGallery())
    }

    private fun setupRecyclerView() {
        adapter = ImageGalleryAdapter.create(context) { uri, isSelected ->
            toggleUriSelection(uri, isSelected)
        }
        binding.recyclerView.setupGrid(3, adapter)
    }

    private fun toggleUriSelection(uri: Uri, isSelected: Boolean) {
        selectedUris.toggle(uri, isSelected)
        binding.btnDone.visibleIf { selectedUris.isNotEmpty() }
    }

    companion object {
        fun show(context: Context, onImagesSelected: (List<Uri>) -> Unit) {
            GalleryBottomSheet(context, onImagesSelected).show()
        }
    }
}
