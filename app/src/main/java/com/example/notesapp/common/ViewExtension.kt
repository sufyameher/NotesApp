package com.example.notesapp.common

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.example.notesapp.R
import com.example.notesapp.common.Helper.hideKeyboard
import com.example.notesapp.common.Helper.showKeyboard
import com.example.notesapp.folder.model.FolderEntity
import com.example.notesapp.note.NoteActionBottomSheet
import com.example.notesapp.note.NoteEntity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlin.collections.filter
import kotlin.collections.forEach

fun View.onClick(listener: () -> Unit) {
    this.setOnClickListener {
        listener()
    }
}

fun View.onClickWithView(action: (View) -> Unit) {
    setOnClickListener { action(it) }
}

fun View.show() {
    visibility = View.VISIBLE
}

fun View.gone() {
    visibility = View.GONE
}

fun View.hideIfVisible() {
    if (this.isVisible) {
        this.gone()
    }
}

fun View.showIfInvisible() {
    if (this.isVisible.not()) {
        this.show()
    }
}

fun View.showOrGone(show: Boolean) = if (show) show() else gone()


fun View.toggleVisibility() {
    this.showOrGone(this.isVisible.not())
}

fun ImageView.setImageDrawable(drawableResId: Int) {
    this.setImageDrawable(ContextCompat.getDrawable(this.context, drawableResId))
}

fun List<TextView>.setTextColor(color: Int) {
    forEach { textView -> textView.setTextColor(color) }
}

fun Context.getColorFromId(@ColorRes colorResId: Int): Int = ContextCompat.getColor(this, colorResId)


fun List<View>.setBackgroundColorAll(colorResId: Int) {
    this.forEach { view ->
        view.setBackgroundColor(view.context.getColorFromId(colorResId))
    }
}

fun View.hidden() {
    visibility = View.INVISIBLE
}

fun View.showOrInvisible(show: Boolean) = if (show) show() else hidden()

fun View.visibleIf(predicate: () -> Boolean) {
    visibility = if (predicate()) View.VISIBLE else View.GONE
}

fun <T> MutableSet<T>.toggle(item: T, add: Boolean) {
    if (add) add(item) else remove(item)
}

fun <T> MutableSet<T>.toggleItem(item: T) {
    if (contains(item)) remove(item) else add(item)
}


inline fun <T : ViewBinding> BottomSheetDialog.inflateBinding(
    crossinline inflater: (LayoutInflater) -> T
): T {
    val binding = inflater(layoutInflater)
    setContentView(binding.root)
    return binding
}

fun RecyclerView.setupGrid(spanCount: Int, adapter: RecyclerView.Adapter<*>) {
    layoutManager = GridLayoutManager(context, spanCount)
    this.adapter = adapter
}

fun Context.dpToPx(dp: Int): Int =
    (dp * resources.displayMetrics.density).toInt()

fun View.setVisible(isVisible: Boolean): View {
    visibility = if (isVisible) View.VISIBLE else View.GONE
    return this
}

fun View.setInvisible(): View {
    visibility = View.INVISIBLE
    return this
}

fun View.setGone(): View {
    visibility = View.GONE
    return this
}


fun ImageView.setImageResourceBy(condition: Boolean, selectedRes: Int, unselectedRes: Int) {
    setImageResource(if (condition) selectedRes else unselectedRes)
}

fun View.onLongClick(listener: () -> Unit) {
    this.setOnLongClickListener {
        listener()
        return@setOnLongClickListener true
    }
}

fun BottomSheetDialogFragment.removeBottomSheetBackground() {
    dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.background = null
}

fun TextView.setTextOrGone(text: String?) {
    this.text = text
    visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
}

fun View.setVisibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

fun View.visibleIfNotEmpty(list: Collection<*>?) {
    visibility = if (list.isNullOrEmpty()) View.GONE else View.VISIBLE
}


fun EditText.showKeyboardAndFocus() {
    requestFocus()
    setSelection(text.length)
    showKeyboard()
}

fun EditText.hideKeyboardAndClearFocus() {
    hideKeyboard()
    clearFocus()
}

fun List<FolderEntity>.visibleFolders(parentId: Int?): List<FolderEntity> =
    this.filter { it.parentFolderId == parentId }

fun View.onClickAndDismiss(note: NoteEntity, sheet: BottomSheetDialogFragment, action: (NoteEntity) -> Unit) {
    setOnClickListener {
        action(note)
        sheet.dismiss()
    }
}

fun TextView.setPinText(isPinned: Boolean) {
    text = if (isPinned) "Unpin" else "Pin"
}

fun ImageView.setPinIcon(isPinned: Boolean) {
    setImageResource(if (isPinned) R.drawable.ic_unpin else R.drawable.ic_pin)
}

fun View.setDrawableBackground(drawableResId: Int) {
    background = context.getDrawable(drawableResId)
}

fun String?.firstLinePreview(maxLength: Int = 100): String =
    this?.lineSequence()?.firstOrNull()?.take(maxLength)?.trim() ?: ""

fun RecyclerView.setupAdapter(adapter: RecyclerView.Adapter<*>) {
    layoutManager = LinearLayoutManager(this.context)
    this.adapter = adapter
}














