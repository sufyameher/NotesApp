package com.example.notesapp.util

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.notesapp.R

class TopBarTitleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CustomTopBarTitle,
            0, 0
        ).apply {
            try {
                val isBold = getBoolean(R.styleable.CustomTopBarTitle_isBold, true)
                setTypeface(null, if (isBold) Typeface.BOLD else Typeface.NORMAL)
            } finally {
                recycle()
            }
        }
    }
}
