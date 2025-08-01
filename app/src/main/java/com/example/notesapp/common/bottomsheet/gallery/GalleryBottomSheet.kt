package com.example.notesapp.common.bottomsheet.gallery

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import com.example.notesapp.databinding.LayoutGalleryBottomSheetBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
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
        binding.btnDone.setOnClickListener {
            onImagesSelected(selectedUris.toList())
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        resizeBottomSheet()
    }

    private fun setupRecyclerView() {
        adapter = ImageGalleryAdapter(context) { uri, isSelected ->
            toggleUriSelection(uri, isSelected)
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(context, 3)
            adapter = this@GalleryBottomSheet.adapter
        }
    }

    private fun resizeBottomSheet(){
        findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.let {
            val behavior = BottomSheetBehavior.from(it)
            val screenHeight = context.resources.displayMetrics.heightPixels

            behavior.peekHeight = (screenHeight * 0.75).toInt()
            it.layoutParams.height = (screenHeight * 0.75).toInt()
            it.requestLayout()
        }
    }

    private fun toggleUriSelection(uri: Uri, isSelected: Boolean) {
        if (isSelected) selectedUris.add(uri) else selectedUris.remove(uri)
        binding.btnDone.visibility = if (selectedUris.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadImagesFromGallery() {
        val uriList = mutableListOf<Uri>()
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null, null,
            sortOrder
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                uriList.add(contentUri)
            }
        }
        adapter.submitList(uriList)
    }


}
