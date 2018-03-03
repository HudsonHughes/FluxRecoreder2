package com.hudson.fluxrecorder

import android.Manifest
import android.support.v4.app.Fragment
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_central.*
import com.anthonycr.grant.PermissionsResultAction
import android.content.Intent
import com.anthonycr.grant.PermissionsManager
import org.jetbrains.anko.alert
import android.util.Log
import io.github.tslamic.prem.*

class CentralActivity : AppCompatActivity(), PremiumerListener {

    val SKU = "extra_storage_space"
    var premium = false
    lateinit var  premiumer : Premiumer
    var currentFragmentIndex : Int = -1
    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_playback -> {
                changeFragment(PlaylistFragment.newInstance())
                currentFragmentIndex = 0
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                changeFragment(MainFragment.newInstance())
                currentFragmentIndex = 1
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_settings -> {
                changeFragment(SettingsFragment.newInstance())
                currentFragmentIndex = 2
                return@OnNavigationItemSelectedListener true

            }
        }
        false
    }

    /**
     * Invoked if sku has been successfully consumed.
     */
    override fun onSkuConsumed() {
        Log.d("Hudson", "onSkuConsumed")

    }

    /**
     * Invoked if sku has not been successfully consumed.
     */
    override fun onFailedToConsumeSku() {
        Log.d("Hudson", "onFailedToConsumeSku")

    }

    /**
     * Invoked if ads should be hidden.
     */
    override fun onHideAds() {
        Log.d("Hudson", "onHideAds")
        premium = true
        if(currentFragmentIndex == 2){
            val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer) as SettingsFragment
            fragment.refreshTimes()
        }


    }

    /**
     * Invoked when purchase details are retrieved.
     *
     * @param purchase [Purchase] instance or `null`, if unavailable.
     */
    override fun onPurchaseDetails(purchase: Purchase?) {
        Log.d("Hudson", "onPurchaseDetails")
    }

    /**
     * Invoked if in-app Billing is unavailable.
     */
    override fun onBillingUnavailable() {
        Log.d("Hudson", "onBillingUnavailable")
    }

    /**
     * Invoked on a purchase request.
     *
     * @param payload a developer-specified [String] containing supplemental information about
     * a purchase.
     */
    override fun onPurchaseRequested(payload: String?) {
        Log.d("Hudson", "onPurchaseRequested")
    }

    /**
     * Invoked on a successful purchase.
     *
     * @param purchase the purchase data.
     */
    override fun onPurchaseSuccessful(purchase: Purchase) {
        Log.d("Hudson", "onPurchaseSuccessful")
        Log.d("Hudson", purchase.developerPayload)
        Log.d("Hudson", purchase.orderId)
        Log.d("Hudson", purchase.packageName)
        Log.d("Hudson", purchase.signature)
        Log.d("Hudson", purchase.sku)
        Log.d("Hudson", purchase.token)
        alert {
            title="Purchase successful"
            message="You can now record for up to than an hour."
        }
    }

    /**
     * Invoked if ads should be visible.
     */
    override fun onShowAds() {
        Log.d("Hudson", "onShowAds")
        premium = false
    }

    /**
     * Invoked when the sku purchase is unsuccessful.
     * This happens if either onActivityResult data is null, or the billing response is invalid.
     *
     * @param data the onActivityResult data, which can be `null`.
     */
    override fun onPurchaseBadResponse(data: Intent?) {
        Log.d("Hudson", "onPurchaseBadResponse")
    }

    /**
     * Invoked when the sku purchase is unsuccessful.
     * This happens if the Activity.onActivityResult resultCode is not equal to
     * Activity.RESULT_OK.
     *
     * @param resultCode the onActivityResult resultCode value.
     * @param data the onActivityResult data.
     */
    override fun onPurchaseBadResult(resultCode: Int, data: Intent?) {
        Log.d("Hudson", "onPurchaseBadResult")
    }

    /**
     * Invoked if in-app Billing is available.
     */
    override fun onBillingAvailable() {
        Log.d("Hudson", "onBillingAvailable")
    }

    /**
     * Invoked if a purchase has failed verification.
     */
    override fun onPurchaseFailedVerification() {
        Log.d("Hudson", "onPurchaseFailedVerification")
    }

    /**
     * Invoked when [SkuDetails] information is retrieved.
     *
     * @param details [SkuDetails] instance or `null`, if an error occurred.
     */
    override fun onSkuDetails(details: SkuDetails?) {
        Log.d("Hudson", "onSkuDetails")
        Log.d("Hudson", details.toString())
    }

    private fun changeFragment(fragment : Fragment) {
        supportFragmentManager.beginTransaction().replace(
                R.id.fragmentContainer, fragment)
                .commit()
    }

    override fun onStart() {
        super.onStart()
        premiumer.bind()
    }

    override fun onStop() {
        super.onStop()
        premiumer.unbind()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (!premiumer.handleActivityResult(requestCode, resultCode, data)) {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_central)

        premiumer = PremiumerBuilder.with(this)
                .sku(SKU)
                .listener(this)
                .build()
        premiumer.skuDetails()
        premiumer.purchaseDetails()
        premiumer.consumeSku()
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

    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        PermissionsManager.getInstance().notifyPermissionsChange(permissions, grantResults)
    }

}

