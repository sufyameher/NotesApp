package com.example.notesapp.common

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.example.notesapp.R
import com.example.notesapp.databinding.ItemPopupIconBinding
import com.example.notesapp.databinding.PopupMenuBinding

class PopupMenuWindow(
    context: Context,
    private val anchor: View,
    private val currentViewMode: ViewMode,
    private val onClickSort: () -> Unit,
    private val onClickViewModeChange: (ViewMode) -> Unit
) {
    private val binding = PopupMenuBinding.inflate(LayoutInflater.from(context))
    private val popupWindow = PopupWindow(
        binding.root,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        ViewGroup.LayoutParams.WRAP_CONTENT,
        true
    ).apply {
        elevation = 10f
        setBackgroundDrawable(ColorDrawable(Color.WHITE))
        isOutsideTouchable = true
    }

    fun show() {
        binding.popupLayout.removeAllViews()
        binding.popupLayout.addView(createItem(anchor.context, R.drawable.ic_sort, "Sort By") {
            onClickSort()
            popupWindow.dismiss()
        })

        val (icon, label, newMode) = when (currentViewMode) {
            ViewMode.LIST -> Triple(R.drawable.ic_grid_view, "Grid View", ViewMode.GRID)
            ViewMode.GRID -> Triple(R.drawable.ic_list_view, "List View", ViewMode.LIST)
        }

        binding.popupLayout.addView(createItem(anchor.context, icon, label) {
            onClickViewModeChange(newMode)
            popupWindow.dismiss()
        })

        popupWindow.showAsDropDown(anchor)
    }

    private fun createItem(context: Context, iconRes: Int, text: String, onClick: () -> Unit): View {
        val itemBinding = ItemPopupIconBinding.inflate(LayoutInflater.from(context))
        itemBinding.ivIcon.setImageResource(iconRes)
        itemBinding.tvTitle.text = text
        itemBinding.root.setOnClickListener { onClick() }
        return itemBinding.root
    }
}
