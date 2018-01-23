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
import android.util.Log
import android.view.Window
import com.android.billingclient.api.BillingClient
import org.jetbrains.anko.doAsync
import java.io.File
import java.io.RandomAccessFile
import com.android.billingclient.api.BillingClient.BillingResponse
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener


class CentralActivity : AppCompatActivity(), PurchasesUpdatedListener {

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
        val mBillingClient : BillingClient = BillingClient.newBuilder(this).setListener(this).build()
        mBillingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(@BillingResponse billingResponseCode: Int) {
                if (billingResponseCode == BillingResponse.OK) {
                    // The billing client is ready. You can query purchases here.

                }
            }

            override fun onBillingServiceDisconnected() {
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.

            }
        })
    }

    override fun onPurchasesUpdated(@BillingResponse responseCode: Int,
                                             purchases: List<Purchase>?) {
        if (responseCode == BillingResponse.OK && purchases != null) {
            for (purchase in purchases) {
                alert("Purchase successful you can now record for up to two hours.").show()
            }
        } else if (responseCode == BillingResponse.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
        } else {
            // Handle any other error codes.
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }
}
