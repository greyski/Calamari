package com.okmoto.calamari

import android.app.Application
import com.okmoto.calamari.audio.PicovoiceRepository

class CalamariApp : Application() {
    override fun onCreate() {
        super.onCreate()
        PicovoiceRepository.init(this)
    }
}

