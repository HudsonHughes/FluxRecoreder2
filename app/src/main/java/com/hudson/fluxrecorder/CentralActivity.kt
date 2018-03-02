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
import org.solovyev.android.checkout.*
import java.lang.Exception
import javax.annotation.Nonnull




class CentralActivity : AppCompatActivity() {

    lateinit var mCheckout : ActivityCheckout
    val premium = "premium"

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

        val billing : Billing = App.getBilling()
        mCheckout = Checkout.forActivity(this, billing)
        mCheckout.start()
        mCheckout.loadInventory(Inventory.Request.create().loadAllPurchases(), InventoryCallback())
    }

    override fun onDestroy() {
        mCheckout.stop()
        super.onDestroy()
    }

    fun startPurchase() {
        mCheckout.startPurchaseFlow(ProductTypes.IN_APP, premium, null, PurchaseListener(this))
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }

    private inner class InventoryCallback : Inventory.Callback {
        override fun onLoaded(products: Inventory.Products) {
            val product = products.get(ProductTypes.IN_APP)
            if (!product.supported) {
                // billing is not supported, user can't purchase anything. Don't show ads in this
                // case
                return
            }
            if (product.isPurchased(premium)) {
                return
            }

        }
    }
}
