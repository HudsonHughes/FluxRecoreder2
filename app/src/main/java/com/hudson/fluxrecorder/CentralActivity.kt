package com.hudson.fluxrecorder

import android.Manifest
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_central.*
import android.widget.Toast
import com.anthonycr.grant.PermissionsResultAction
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.content.Context
import com.anthonycr.grant.PermissionsManager
import org.jetbrains.anko.alert
import org.jetbrains.anko.yesButton
import android.support.annotation.NonNull
import android.view.Window
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile


class CentralActivity : AppCompatActivity() {

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_playback -> {
                changeFragment(PlaylistFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                changeFragment(MainFragment.newInstance())
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                changeFragment(SettingsFragment.newInstance())
                return@OnNavigationItemSelectedListener true

            }
        }
        false
    }

    private fun changeFragment(fragment : Fragment) {
        supportFragmentManager.beginTransaction().replace(
                R.id.fragmentContainer, fragment)
                .commit()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_central)
        PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.RECEIVE_BOOT_COMPLETED), object : PermissionsResultAction() {

            override fun onGranted() {
                navigation.selectedItemId = R.id.navigation_dashboard
                changeFragment(MainFragment.newInstance())
            }

            override fun onDenied(permission: String) {
                alert("All requested permissions must be granted before the app can function properly. Re open the app to accept the request or accept them through the device settings.", "Permission not granted") {
                    positiveButton("Exit") { System.exit(0) }
                }.show()
            }
        })

        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        doAsync {

            for(file in App.instance.folder.listFiles()){
                var target = File(file.absolutePath + ".wav")
                file.copyTo(target)
                var wavHeader = WavHeader(RandomAccessFile(target, "rw"), 44100, 1, 16)
                wavHeader.writeHeader()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }
}
