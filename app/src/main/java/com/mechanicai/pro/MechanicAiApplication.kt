package com.mechanicai.pro

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.mechanicai.pro.BuildConfig
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Initializes Firebase and Hilt.
 */
@HiltAndroidApp
class MechanicAiApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        installAppCheck()
        Log.i(TAG, "Mechanic AI Pro initialized")
    }

    private fun installAppCheck() {
        val factory = if (BuildConfig.DEBUG) {
            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(factory)
    }

    companion object {
        private const val TAG = "MechanicAiApplication"
    }
}
