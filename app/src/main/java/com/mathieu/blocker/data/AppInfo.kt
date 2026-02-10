package com.mathieu.blocker.data

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean = false
)
