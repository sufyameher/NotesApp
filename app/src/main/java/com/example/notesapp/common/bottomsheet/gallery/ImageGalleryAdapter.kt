package com.example.notesapp.common.bottomsheet.gallery

import android.content.Context

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.notesapp.R
import com.example.notesapp.common.onClick
import com.example.notesapp.common.setImageResourceBy
import com.example.notesapp.common.setVisible
import com.example.notesapp.common.visibleIf

class ImageGalleryAdapter(
    private val context: Context,
    private val onImageSelected: (Uri, Boolean) -> Unit
) : ListAdapter<Uri, ImageGalleryAdapter.ImageViewHolder>(DIFF_CALLBACK) {

    private val selectedItems = mutableSetOf<Uri>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val uri = getItem(position)
        holder.bind(uri)
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image = itemView.findViewById<ImageView>(R.id.image)
        private val overlay = itemView.findViewById<View>(R.id.overlay)
        val checkmark = itemView.findViewById<ImageView>(R.id.checkmark)

        fun bind(uri: Uri) {
            Glide.with(context).load(uri).into(image)

            val isSelected = selectedItems.contains(uri)

            checkmark.setImageResourceBy(isSelected, R.drawable.ic_image_selected, R.drawable.ic_image_unselected)
            overlay.setVisible(isSelected)

            itemView.onClick {
                val wasSelected = selectedItems.contains(uri)
                if (wasSelected) {
                    selectedItems.remove(uri)
                } else {
                    selectedItems.add(uri)
                }
                notifyItemChanged(bindingAdapterPosition)
                onImageSelected(uri, !wasSelected)
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Uri>() {
            override fun areItemsTheSame(old: Uri, new: Uri) = old == new
            override fun areContentsTheSame(old: Uri, new: Uri) = old == new
        }

        fun create(
            context: Context,
            onImageSelected: (Uri, Boolean) -> Unit
        ): ImageGalleryAdapter {
            return ImageGalleryAdapter(context, onImageSelected)
        }
    }
}
