package com.hudson.fluxrecorder

import android.app.ActivityManager
import android.app.Application
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.support.v4.app.NotificationCompat
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
        folder = this.filesDir.resolve("Buffer_Store_Dont_Touch")
        //folder = Environment.getExternalStorageDirectory().resolve("Downloads")
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

    fun clearBuffer() {
        folder.walkTopDown().forEach {
            it.delete()
        }
    }
    public fun canHandleBufferDuration(duration : Int) : Boolean {
        val usableSpace = 30 * 60 * bytesPerSecond
        val neededSpace = (duration + 30) * bytesPerSecond
        val alreadyHave = folder.listFiles().sumBy { it.length().toInt() }.toLong()
        if(usableSpace + alreadyHave >= neededSpace)
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
            RandomAccessFile(file, "rw").setLength(30 * bytesPerSecond.toLong())
            file = folder.resolve("${System.currentTimeMillis() / 1000}.raw")
            file.appendBytes(bytes)
            while(folder.listFiles().size > (secondsDesired / 30) + 1) {
                Log.d("Hudson", "Deleted ${oldestBufferFile.name}")
                oldestBufferFile.delete()
            }
        }else{
            file.appendBytes(bytes)
        }
        return
    }

    fun refreshBufferSize() : Long {
        var result = 0L
        for (element in folder.listFiles()) result += element.length()
        totalBufferSize = result
        Log.d("Hudson", "refreshed size " + totalBufferSize.toString())
        return result
    }

    public inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
        var sum: Long = 0L
        for (element in this) {
            sum += selector(element)
        }
        return sum
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
                var size = 0L
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
            get() {
                Log.d("Hudson", "Buffer Size " + instance.refreshBufferSize().toString() + " bytesPerSecond " + bytesPerSecond)
                return instance.refreshBufferSize().toInt() / bytesPerSecond
            }
            private set
        var secondsDesired : Int
            get() = instance.defaultSharedPreferences.getString("seconds_desired", "120").toInt()
            set(value) {
                instance.defaultSharedPreferences.edit().putString("seconds_desired", value.toString()).apply()
            }
        var storagePath : String
            get() {
                val storage_folder = Environment.getExternalStorageDirectory().resolve("FluxRecorder")
                if(storage_folder.exists() && storage_folder.isFile){
                    storage_folder.resolve("FluxRecorder").renameTo(Environment.getExternalStorageDirectory().resolve("FluxRecorder_old"))
                }
                if(!storage_folder.exists()) storage_folder.mkdir()
                return instance.defaultSharedPreferences.getString("storage_path", storage_folder.path)
            }
            set(value) {
                instance.defaultSharedPreferences.edit().putString("storage_path", value).apply()
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
            for (service in manager.getRunningServices(Integer.MAX_VALUE))
                if (RecordingService::class.java.canonicalName == service.service.className){
                    Log.d("Hudson", "Service is running")
                    return true
                }
            Log.d("Hudson", "Service is not running")
            return false
        }
        fun startService() : Boolean {
            var mic = testMicrophone()
            if(mic) {
                if (Build.VERSION.SDK_INT >= 26) {
                    instance.startForegroundService(Intent(instance, RecordingService::class.java))
                }else{
                    instance.startService(Intent(instance, RecordingService::class.java))
                }
            }else{
                val mNotificationManager = instance.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notificationIntent = Intent(instance, CentralActivity::class.java)
                val pendingIntent = PendingIntent.getActivity(instance, 0, notificationIntent, 0)
                val notification = NotificationCompat.Builder(instance, "ShadowRecorder")
                        .setContentTitle("Recording Interrupted")
                        .setContentText("Unable to acquire control of the microphone.")
                        .setSmallIcon(R.drawable.ic_stat_error)
                        .setContentIntent(pendingIntent)
                        .build()
                mNotificationManager.notify(415, notification)
                return false
            }

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