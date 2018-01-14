package com.fluxrecorder.hudsonhughes.fluxrecorder

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import org.jetbrains.anko.defaultSharedPreferences
import java.io.*
import kotlin.system.measureTimeMillis


/**
 * Created by Hudson Hughes on 12/16/2017.
 */
class App : Application() {
    lateinit var folder : File
    override fun onCreate() {
        super.onCreate()
        folder = this.filesDir.resolve("Buffer_Store_Don't_Touch")
        folder = Environment.getExternalStorageDirectory().resolve("Downloads")
        folder.mkdir()
        instance = this
    }

    var oldestBufferFile : File = File("")
        get(): File {
            if (folder.listFiles().isEmpty()) return folder.resolve("${System.currentTimeMillis() / 1000}.raw")
            return folder.listFiles().sortedBy { file -> file.name } [0]
        }
        private set

    var newestBufferFile : File = File("")
        get(): File {
            if (folder.listFiles().isEmpty()) return folder.resolve("${System.currentTimeMillis() / 1000}.raw")
            return folder.listFiles().sortedByDescending { file -> file.name } [0]
        }
        private set
    public fun clearBuffer() {
        folder.walkTopDown().forEach {
            it.delete()
        }
    }
    public fun canHandleBufferDuration(duration : Int) : Boolean {
        if(folder.usableSpace + folder.listFiles().sumBy { it.length().toInt() }.toLong() > (duration + 30) * bytesPerSecond)
            return true
        return false
    }

    public fun requiredSpace(duration : Int) : Long {
        if(canHandleBufferDuration(duration))
        { return 0 }
        return ((duration + 30) * bytesPerSecond - folder.listFiles().sumBy { it.length().toInt() }.toLong())
    }

    public fun canHandleWavFileDuration(duration : Int) : Boolean {
        if(folder.usableSpace > duration * bytesPerSecond + 44)
            return true
        return false
    }

    public fun writeBuffer(bytes : ByteArray) {
        if(folder.usableSpace < secondsDesired * bytesPerSecond + bytesPerSecond * 30) throw FileSystemException(folder)
        var file : File = newestBufferFile
        if(file.length() >= 30 * bytesPerSecond) {
            Log.d("Hudson","Finished with ${file.name} ${file.length()}")
            RandomAccessFile(file, "rw").setLength(30 * bytesPerSecond.toLong())
            file = folder.resolve("${System.currentTimeMillis() / 1000}.raw")
            file.appendBytes(bytes)
            for(i in folder.listFiles()) Log.d("Hudson", i.name)
            while(folder.listFiles().size > ((secondsDesired / 60) * 2) + 1)
                oldestBufferFile.delete()
        }else{
            file.appendBytes(bytes)
        }
        return
    }

    fun refreshBufferSize() : Long {
        Log.d("Hudson", folder.canonicalPath)
        val du : Process = Runtime.getRuntime().exec("/system/bin/du " + folder.canonicalFile, arrayOf<String>(), Environment.getRootDirectory())
        val br = BufferedReader(InputStreamReader(du.inputStream)).readLine()
        Log.d("Hudson", br)
        var result = br.split("\t")[0].toLong()
        totalBufferSize = result
        return result
    }

    companion object {
        val maximumTime : String = "maximumTime"
        val bufferSize : String = "bufferSize"

        lateinit var instance: App
            private set

        var maxTime : Int
            get() = instance.defaultSharedPreferences.getInt(maximumTime, 2)
            set(value) {
                instance.defaultSharedPreferences.edit().putInt(maximumTime, value).apply()
            }
        var recorderRunning : Boolean
            get() = instance.defaultSharedPreferences.getBoolean(maximumTime, false)
            set(value) {
                instance.defaultSharedPreferences.edit().putBoolean(maximumTime, value).apply()
            }
        var totalBufferSize : Long
            get() {
                Log.d("Hudson", "Time to get Folder ordered " + measureTimeMillis { instance.folder.listFiles().sortedByDescending {file -> file.lastModified() } })
                Log.d("Hudson", "Time to write " + measureTimeMillis { instance.folder.resolve("aa").appendBytes(kotlin.ByteArray(400)) })
                var size = 0L
                Log.d("Hudson" , "Time to get size " + measureTimeMillis { size = instance.folder.resolve("aa").length() } )
                Log.d("Hudson", "size ${size}")
                var result = instance.defaultSharedPreferences.getLong(bufferSize, -1)
                if (result == -1L){
                    val du : Process = Runtime.getRuntime().exec("/system/bin/du " + instance.folder.canonicalFile, arrayOf<String>(), Environment.getRootDirectory())
                    val br = BufferedReader(InputStreamReader(du.inputStream)).readLine()
                    Log.d("Hudson", br)
                    result = br.split("\t")[0].toLong()
                    totalBufferSize = result
                }
                return result
            }
            set(value) {
                instance.defaultSharedPreferences.edit().putLong(bufferSize, value).apply()
            }
        var bytesPerSecond : Int = 0
            get() = 1 * 44100 * 2 * 1
            private set
        var bufferDuration : Int = 0
            get() = instance.refreshBufferSize().toInt() / bytesPerSecond
            private set
        var secondsDesired : Float
            get() = instance.defaultSharedPreferences.getFloat("seconds_desired", 120.0f)
            set(value) {
                instance.defaultSharedPreferences.edit().putFloat("seconds_desired", value).apply()
            }
        var storagePath : String
            get() = instance.defaultSharedPreferences.getString("storagePath", Environment.getExternalStorageDirectory().absolutePath)
            set(value) {
                instance.defaultSharedPreferences.edit().putString("storagePath", value).apply()
            }
        fun testMicrophone() : Boolean {
            var N = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            val recorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                    44100,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    N * 10)
            recorder.startRecording()
            var buffer = ByteArray(N * 10)
            N = recorder.read(buffer, 0, buffer.size)
            try{
                recorder.stop()
                recorder.release()
            }catch(e : Throwable){

            }
            return N > 0
        }
        @SuppressWarnings("deprecation")
        fun isServiceRunning(): Boolean {
            val manager = instance.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (i : ActivityManager.RunningServiceInfo in manager.getRunningServices(Integer.MAX_VALUE) )
                Log.d("Hudson", "Service running ${i}")
            for (service in manager.getRunningServices(Integer.MAX_VALUE))
                if (RecordingService::class.java.canonicalName == service.service.className)
                    return true
            return false
        }
        fun startService() : Boolean {
            if(testMicrophone())
                instance.startService(Intent(instance, RecordingService::class.java))
            else
                return false
            return isServiceRunning()
        }
        fun stopService() : Boolean {
            instance.stopService(Intent(instance, RecordingService::class.java))
            return isServiceRunning()
        }
        fun toggleService() : Boolean {
            if ( isServiceRunning() )
                return stopService()
            return startService()
        }

        fun resizeBuffer(new_seconds_desired : Int) : Boolean {
            val size_deisred = new_seconds_desired * bytesPerSecond
            if(size_deisred < secondsDesired * bytesPerSecond){
                while(instance.folder.listFiles().size > ((secondsDesired / 60) * 2) + 1)
                    instance.oldestBufferFile.delete()
            }
            return true
        }
    }
}