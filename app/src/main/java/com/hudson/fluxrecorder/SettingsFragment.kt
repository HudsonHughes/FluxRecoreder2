package com.hudson.fluxrecorder

import android.content.ComponentName
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.preference.Preference
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.support.v7.preference.CheckBoxPreference
import android.support.v7.preference.ListPreference
import android.util.Log
import com.hudson.fluxrecorder.App.Companion.resizeBuffer
import com.takisoft.fix.support.v7.preference.PreferenceFragmentCompat
import org.jetbrains.anko.noButton
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.defaultSharedPreferences
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.yesButton
import android.content.Intent
import android.net.Uri
import org.w3c.dom.ls.LSException
import java.util.concurrent.atomic.AtomicLongArray


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [SettingsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class SettingsFragment : PreferenceFragmentCompat(), SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var buffer_size_list : ListPreference
    fun refreshTimes(){

        buffer_size_list.entries = if ((activity as CentralActivity).premium) arrayOf("1 minute", "2 minutes", "5 minutes", "15 minutes", "30 minutes", "45 minutes", "60 minutes") else arrayOf("1 minute", "2 minutes", "5 minutes")
        buffer_size_list.entryValues = if ((activity as CentralActivity).premium) arrayOf("60", "120", "300", "900", "1800", "2700", "3600") else arrayOf("60", "120", "300")
    }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.pref_general, rootKey)
        val activation_switch = findPreference("on_off")
        buffer_size_list = findPreference("seconds_desired") as ListPreference
        activation_switch.onPreferenceChangeListener = android.support.v7.preference.Preference.OnPreferenceChangeListener{
            preference, newValue ->
            if (newValue as Boolean) {
                if (App.startService())
                    true
                else {
                    Log.d("Hudson", "Unable to start service startService failed.")
                    false
                }
            }else{
                App.stopService()
                true
            }
        }
        (activation_switch as CheckBoxPreference).isChecked = App.isServiceRunning()
        buffer_size_list.entries = if ((activity as CentralActivity).premium) arrayOf("1 minute", "2 minutes", "5 minutes", "15 minutes", "30 minutes", "45 minutes", "60 minutes") else arrayOf("1 minute", "2 minutes", "5 minutes")
        buffer_size_list.entryValues = if ((activity as CentralActivity).premium) arrayOf("60", "120", "300", "900", "1800", "2700", "3600") else arrayOf("60", "120", "300")
        buffer_size_list.setOnPreferenceChangeListener { preference, newValue ->
            if(!App.instance.canHandleBufferDuration((newValue as String).toInt())) {
                alert("Buffer resize failed. Try freeing some space or using a smaller buffer size.") { positiveButton("Close") {} }.show()
            }else{
                if(newValue.toInt() > App.secondsDesired && App.instance.canHandleBufferDuration((newValue as String).toInt())){
                    resizeBuffer((newValue as String).toInt())
                    App.secondsDesired = (newValue as String).toInt()
                    (preference as ListPreference).value = App.secondsDesired.toString()
                }else{
                    alert(message = "If you are shrinking the buffer data older than the new buffer length will be deleted.", title = "Are you sure?"){
                        positiveButton("Yes"){
                            resizeBuffer(newValue.toInt())
                            App.secondsDesired = newValue.toInt()
                            (preference as ListPreference).value = App.secondsDesired.toString()
                        }
                        negativeButton("Cancel"){

                        }
                    }.show()
                }
                false
            }
            false

        }
        val button = findPreference("clear_buffer_button")
        button.setOnPreferenceClickListener {
            alert("All buffer data that hasn't been saved for later playback will be deleted.", "Are you sure?"){
                positiveButton("Yes") { App.instance.clearBuffer(); toast("Buffer cleared") }
                negativeButton("No"){}
            }.show()
            false
        }
        val not_button = findPreference("notification")
        not_button.setOnPreferenceClickListener {
            alert("Google has made it mandatory that background services such as Retroactive Recorder have a notification attached to them. This keeps rogue apps from stealing resources without your knowledge.", "You can't"){
                positiveButton("Close") {  }
            }.show()
            false
        }

        val premium = findPreference("premium")
        premium.setOnPreferenceClickListener {
            alert("By purchasing the premium add-on you will be able to record up to 1 hour of audio", "Go Premium"){
                positiveButton("Buy") {
                    (activity as CentralActivity).premiumer.purchase(activity)
                }
                negativeButton("Not now") { }
            }.show()
            false
        }
        premium.isEnabled = !(activity as CentralActivity).premium

        val mic_busted = findPreference("micBusted")
        mic_busted.setOnPreferenceClickListener {
            alert("Only one app can take hold of the microphone at a time. Retroactive Recorder cannot be used in tandem with voice command apps like OK Google or chat apps like Skype.", ""){
                positiveButton("Close") {  }
            }.show()
            false
        }

        val shareOnPlay = findPreference("shareApp")
        shareOnPlay.setOnPreferenceClickListener {
            val sendIntent = Intent()
            sendIntent.action = Intent.ACTION_SEND
            sendIntent.putExtra(Intent.EXTRA_TEXT,
                    "A recorder with premonition: https://play.google.com/store/apps/details?id=com.hudson.fluxrecorder")
            sendIntent.type = "text/plain"
            startActivity(sendIntent)
            false
        }

        val rateOnPlay = findPreference("rateOnPlay")
        rateOnPlay.setOnPreferenceClickListener {
            val appPackageName = context!!.packageName // getPackageName() from Context or Activity object
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
            } catch (anfe: Exception) {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
            }

            false
        }
    }
    override fun onResume() {
        super.onResume()
        // Set up a listener whenever a key changes
        preferenceScreen.sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }
    override fun onPause() {
        super.onPause()
        // Unregister the listener whenever a key changes
        preferenceScreen.sharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if(key == "on_off"){
            val activation_switch = findPreference("on_off") as CheckBoxPreference
            activation_switch.isChecked = sharedPreferences.getBoolean("on_off", false)
            sharedPreferences.edit().putBoolean("suppose", sharedPreferences.getBoolean("on_off", false)).commit()
        }
        if(key == "reboot"){
            val reboot = findPreference("reboot")
            val receiver : ComponentName = ComponentName(context, OnBootComplete::class.java)
            val pm : PackageManager = context!!.packageManager
            if(sharedPreferences.getBoolean("reboot", false)){
                pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            }else{
                pm.setComponentEnabledSetting(receiver,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            }
            Log.d("Hudson", "Rebooting " + sharedPreferences.getBoolean("reboot", false))
        }
    }
    companion object {
        @JvmStatic
        fun newInstance() = SettingsFragment().apply {
            arguments = Bundle().apply {
            }
        }
    }
}
