/**
 * Small helpers for working with the overlay bubble views.
 *
 * Methodology:
 * - Keeps `WindowManager` attach/remove logic in one place so bubble/prompt Compose views
 *   can simply call `addToWindow(...)` / `removeFromWindow(...)`.
 * - The operations are nullable-wrapped because tests/services may not have a `WindowManager`.
 */
package com.okmoto.calamari.overlay

import android.view.View
import android.view.WindowManager

internal fun View.addToWindow(params: WindowManager.LayoutParams, wm: WindowManager?) {
    wm?.addView(this, params)
}

internal fun View.removeFromWindow(wm: WindowManager?) {
    wm?.removeView(this)
}
