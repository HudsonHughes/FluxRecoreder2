package com.hudson.fluxrecorder

import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.IBinder
import android.util.Log
import org.greenrobot.eventbus.EventBus
import android.media.MediaRecorder
import android.R.attr.process
import android.app.*
import android.media.MediaRecorder.AudioSource
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.graphics.Color
import android.os.Build
import android.support.annotation.RequiresApi
import org.jetbrains.anko.defaultSharedPreferences


class RecordingService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    lateinit var audioIn : AudioIn
    lateinit var mNotificationManager : NotificationManager
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        Log.d("Hudson", "service onCreate")
//        EventBus.getDefault().register(this);
        val notificationIntent = Intent(this, CentralActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)
        mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val id = "ShadowRecorder"
        // The user-visible name of the channel.
        val name = "Retroactive Recorder"
        // The user-visible description of the channel.
        val description = "Retroactive Recorder"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        if(Build.VERSION.SDK_INT >= 26){
            val mChannel = NotificationChannel(id, name, importance)
            // Configure the notification channel.
            mChannel.description = description
            mChannel.enableVibration(false)
            mNotificationManager.createNotificationChannel(mChannel)
        }

        val notification = NotificationCompat.Builder(this, "ShadowRecorder")
                .setContentTitle("Retroactive Recorder Running")
                .setContentText("Click to open app")
                .setSmallIcon(R.drawable.ic_stat_icon)
                .setContentIntent(pendingIntent)
                .build()
        startForeground(414, notification)

        //mNotificationManager.notify(414, notification)
        defaultSharedPreferences.edit().putBoolean("on_off", true).commit()
        App.recorderRunning = true
        audioIn = AudioIn(this, pendingIntent, { stopSelf() })
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("Hudson", "service onDestroy")
        App.recorderRunning = false
        audioIn.close()

        if(Build.VERSION.SDK_INT >= 24)
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
        defaultSharedPreferences.edit().putBoolean("on_off", false).commit()
    }

    class AudioIn(ctx : Context, pendingIntent: PendingIntent, callback: () -> Unit) : Thread() {
        var stopped = false
        lateinit var ctx : Context;
        lateinit var callback : () -> Unit;
        lateinit var pendingIntent : PendingIntent
        init {
            this.ctx = ctx
            this.callback = callback
            this.pendingIntent = pendingIntent
            start()
        }

        override fun run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO)
            var recorder: AudioRecord? = null
            val buffers = Array(256) { ByteArray(160) }
            var ix = 0
            val mNotificationManager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            try { // ... initialise

                var N = AudioRecord.getMinBufferSize(App.sampleRate, App.channelCount, App.encoding)
                recorder = AudioRecord(AudioSource.MIC,
                        App.sampleRate,
                        App.channelConfiguration,
                        App.encoding,
                        N * 10)
                recorder.startRecording()

                while (!stopped) {
                    val buffer = buffers[ix++ % buffers.size]
                    N = recorder.read(buffer, 0, buffer.size)
                    if(N < 1) {
                        Log.d("Hudson", "Error message after trying read(): $N")
                        val notification = NotificationCompat.Builder(ctx, "ShadowRecorder")
                                .setContentTitle("Unable to Record")
                                .setContentText("An error has occurred that keeps us from getting data from the mic.")
                                .setSmallIcon(R.drawable.ic_stat_error)
                                .setContentIntent(pendingIntent)
                                .build()
                        mNotificationManager.notify(415, notification)
                        break
                    } else {
                        if (buffer.size != N)
                            Log.d("Hudson", "Result from read(): $N")
                        App.instance.writeBuffer(buffer)
                    }
                    //process is what you will do with the data...not defined here
                }
            }catch (x : FileSystemException) {
                val notification = NotificationCompat.Builder(ctx, "ShadowRecorder")
                        .setContentTitle("Recording Interrupted")
                        .setContentText("There is not enough space for a full audio buffer.")
                        .setSmallIcon(R.drawable.ic_stat_error)
                        .setContentIntent(pendingIntent)
                        .build()
                mNotificationManager.notify(415, notification)
            } catch (x: Throwable) {
                Log.w("Hudson", "Error reading voice audio", x)
            } finally {
                    if(recorder != null) {
                        recorder.stop()
                        recorder.release()
                    }

                close()
                callback()
            }
        }

        fun close() {
            stopped = true

        }

    }
}
