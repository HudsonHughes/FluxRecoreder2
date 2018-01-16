package com.hudson.fluxrecorder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import org.jetbrains.anko.defaultSharedPreferences

class OnBootComplete : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Hudson", "got some")
            Log.d("Hudson", "got boot")
            //Write her your code , what you want.
            if(context.defaultSharedPreferences.getBoolean("suppose", false) && !App.isServiceRunning() && context.defaultSharedPreferences.getBoolean("reboot", false)){
                Log.d("Hudson", "got boot starting service")
                App.startService()
            }
        }
}
