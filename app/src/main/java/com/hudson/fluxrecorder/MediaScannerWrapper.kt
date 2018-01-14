package com.hudson.fluxrecorder

import android.content.Context
import android.media.MediaScannerConnection.scanFile
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.MediaScannerConnectionClient
import android.net.Uri
import android.util.Log


/**
 * Created by Hudson Hughes on 1/14/2018.
 */
class MediaScannerWrapper(ctx: Context, private val mPath: String, private val mMimeType: String) : MediaScannerConnectionClient {

    private val mConnection: MediaScannerConnection

    init {
        mConnection = MediaScannerConnection(ctx, this)
    }

    fun scan() {
        mConnection.connect()
    }

    override fun onMediaScannerConnected() {
        mConnection.scanFile(mPath, mMimeType)
        Log.d(javaClass.name, "Media file scanned: " + mPath)
    }

    override fun onScanCompleted(arg0: String, arg1: Uri) {

    }

}