package com.okmoto.calamari.overlay

import android.content.Context
import android.view.View
import android.view.WindowManager
import android.widget.Toast

internal fun View.addToWindow(params: WindowManager.LayoutParams, wm: WindowManager?) {
    wm?.addView(this, params)
}

internal fun View.removeFromWindow(wm: WindowManager?) {
    wm?.removeView(this)
}

internal fun Context.makeToast(text: String) {
    Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
}
