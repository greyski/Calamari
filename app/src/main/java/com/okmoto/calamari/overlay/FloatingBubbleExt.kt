package com.okmoto.calamari.overlay

import android.view.View
import android.view.WindowManager

internal fun View.addToWindow(params: WindowManager.LayoutParams, wm: WindowManager?) {
    wm?.addView(this, params)
}

internal fun View.removeFromWindow(wm: WindowManager?) {
    wm?.removeView(this)
}
