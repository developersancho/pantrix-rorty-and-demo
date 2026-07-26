package com.developersancho.pantrixrortyanddemo.feature.lab

import android.content.Context
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.google.android.material.button.MaterialButton

/**
 * The Lab screens are built in code, not XML: they are a flat list of "title / subtitle / do this"
 * rows, and one row per SDK call reads better as a loop than as fifty near-identical layout blocks.
 */
internal fun LinearLayout.sectionHeader(title: String) {
    addView(TextView(context).apply {
        text = title
        textSize = 13f
        alpha = 0.7f
        setPadding(0, 28, 0, 8)
    })
}

internal fun LinearLayout.actionRow(title: String, subtitle: String, onClick: () -> Unit) {
    addView(MaterialButton(context).apply {
        text = title
        isAllCaps = false
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setOnClickListener { onClick() }
    })
    addView(TextView(context).apply {
        text = subtitle
        textSize = 12f
        alpha = 0.6f
        setPadding(8)
    })
}

internal fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
