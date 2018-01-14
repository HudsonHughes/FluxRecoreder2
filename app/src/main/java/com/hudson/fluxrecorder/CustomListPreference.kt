package com.hudson.fluxrecorder

import android.content.Context
import android.content.SharedPreferences
import android.preference.ListPreference
import android.text.TextUtils
import org.jetbrains.anko.alert
import org.jetbrains.anko.noButton
import org.jetbrains.anko.yesButton
import android.os.Build
import android.support.annotation.RequiresApi
import android.util.AttributeSet


/**
 * Created by VGDCUser on 1/8/2018.
 */

class CustomListPreference : ListPreference {

    constructor(context : Context, attrs : AttributeSet, defStyleAttr : Int, defStyleRes : Int) : super(context, attrs, defStyleAttr, defStyleRes) {}
    constructor(context : Context, attrs : AttributeSet, defStyleAttr : Int) : super(context, attrs, defStyleAttr) {}
    constructor(context : Context, attrs : AttributeSet) : super(context, attrs) {}
    constructor(context : Context) : super(context) {}

    var changed = false
    var holder = super.getValue()

    override fun onDialogClosed(positiveResult: Boolean) {
        if(positiveResult){
            holder = super.getValue()
        }
        super.onDialogClosed(positiveResult)
        if(positiveResult){
            var newValue = super.getValue()
            super.setValue(holder)
            if(holder != newValue){
                if (!App.instance.canHandleBufferDuration(newValue.toInt())) {
                    context.alert("Resize Failed", context.getString(R.string.error_space) ).show()
                } else if (true) {
                    context.alert( title = "Alter the buffer size?", message = "If you are shrinking the buffer, This will cause older audio to be deleted." ) {
                        yesButton {
                            super.setValue(newValue)
                            notifyChanged()
                        }
                        noButton { }
                    }.show()
                }
            }
        }
    }
}