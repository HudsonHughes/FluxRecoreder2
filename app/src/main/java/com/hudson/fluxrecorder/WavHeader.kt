package com.hudson.fluxrecorder

/**
 * Created by Hudson Hughes on 12/23/2017.
 */
import android.util.Log

import java.io.RandomAccessFile

/**
 * Created by Hudson Hughes on 9/19/2017.
 */

class WavHeader(internal var randomAccessFile: RandomAccessFile, internal var mSampleRate: Int, internal var mChannels: Int, internal var mSampleBits: Int) {

    @Throws(Exception::class)
    fun writeHeader() {
        var file: RandomAccessFile? = randomAccessFile
        file!!.seek(0)


        val bytesPerSec = (mSampleBits + 7) / 8

        file.writeBytes("RIFF") // WAV chunk header
        file.writeInt(Integer.reverseBytes(file.length().toInt() - 8)) // WAV chunk size
        file.writeBytes("WAVE") // WAV format

        file.writeBytes("fmt ") // format subchunk header
        file.writeInt(Integer.reverseBytes(16)) // format subchunk size
        file.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // audio format
        file.writeShort(java.lang.Short.reverseBytes(mChannels.toShort()).toInt()) // number of channels
        file.writeInt(Integer.reverseBytes(mSampleRate)) // sample rate
        file.writeInt(Integer.reverseBytes(mSampleRate * mChannels * bytesPerSec)) // byte rate
        file.writeShort(java.lang.Short.reverseBytes((mChannels * bytesPerSec).toShort()).toInt()) // block align
        file.writeShort(java.lang.Short.reverseBytes(mSampleBits.toShort()).toInt()) // bits per sample

        file.writeBytes("data") // data subchunk header
        file.writeInt(Integer.reverseBytes(file.length().toInt() - 44)) // data subchunk size
        Log.d("Hudson", "Wave file size = " + (file.length().toInt() - 44))
        file.close()
        file = null
    }
}