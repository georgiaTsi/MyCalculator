package com.example.calculator

import android.content.Context
import android.text.InputFilter
import android.text.Spanned
import android.widget.Toast

class MaxLengthInputFilter(private val maxLength: Int, private val context: Context) : InputFilter {

    override fun filter(source: CharSequence?, start: Int, end: Int,
        dest: Spanned?, dstart: Int, dend: Int): CharSequence? {
        val currentLength = dest?.length ?: 0
        val keep = maxLength - (currentLength - (dend - dstart))

        if (keep <= 0) {
            Toast.makeText(context, "Maximum length reached", Toast.LENGTH_SHORT).show()
            return ""
        } else if (keep >= end - start) {
            return null // Keep original input
        } else {
            val kept = keep.coerceAtMost(end - start)
            return source?.subSequence(start, start + kept)
        }
    }
}