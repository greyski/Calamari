package com.okmoto.calamari.overlay

import androidx.annotation.ColorRes
import com.okmoto.calamari.R

/**
 * High-level listening lifecycle state for the floating bubble service.
 */
enum class ListeningState(
    @param:ColorRes val textColorId: Int,
    @param:ColorRes val backgroundColorId: Int,
    val notificationText: String = "Say \"Calamari\"!",
) {
    IDLE(
        textColorId = R.color.black,
        backgroundColorId = R.color.calamari_bubble_idle_gray,
        notificationText = "Sleeping...",
    ),
    AWAKE(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_awake_purple,
    ),
    AWAITING_EVENT(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_awaiting_event_pink,
    ),
    IDLE_TITLE(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_idle_title_blue,
    ),
    AWAITING_TITLE(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_awaiting_title_yellow,
    ),
    IDLE_SEND(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_awaiting_send_green,
    ),
    ERROR(
        textColorId = R.color.white,
        backgroundColorId = R.color.calamari_bubble_error_red,
    ),
}

