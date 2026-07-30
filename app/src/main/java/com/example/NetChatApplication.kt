package com.example

import android.app.Application
import com.example.data.remote.FirebaseManager

class NetChatApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseManager.init(this)
    }
}
