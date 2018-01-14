package com.fluxrecorder.hudsonhughes.fluxrecorder


import android.app.AlertDialog
import android.app.Dialog
import android.app.DialogFragment
import android.content.DialogInterface
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import kotlinx.android.synthetic.main.player_dialog.view.*
import java.util.concurrent.TimeUnit
import javax.xml.datatype.DatatypeConstants.MINUTES
import android.view.Window.FEATURE_NO_TITLE
import kotlinx.android.synthetic.main.player_dialog.*
import java.io.File
import android.view.WindowManager
import android.R.drawable.ic_media_play
import android.R.drawable.ic_media_pause
import android.util.Log
import java.io.IOException


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_filePath = "filePath"

/**
 * A simple [Fragment] subclass.
 * Use the [PlayerFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class PlayerFragment : DialogFragment() {
    // TODO: Rename and change types of parameters
    private var filePath: String? = null

    private var mMediaPlayer : MediaPlayer? = MediaPlayer()
    private var mHandler : Handler = Handler()
    private lateinit var mSeekBar : SeekBar
    private lateinit var textViewFilename : TextView
    private lateinit var textViewCurrentTime : TextView
    private lateinit var textViewMaxTime : TextView
    private lateinit var buttonPlay: FloatingActionButton
    private lateinit var buttonBack : FloatingActionButton
    private lateinit var buttonNext : FloatingActionButton
    private var isPlaying = false
    private var minutes = 0L
    private var seconds = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            filePath = it.getString(ARG_filePath)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        val inflated = inflater.inflate(R.layout.player_dialog, container, false)

        return inflated
    }

    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        val builder = AlertDialog.Builder(activity)
        val view = activity.layoutInflater.inflate(R.layout.player_dialog, null)
        mSeekBar = view.seekBarMusic
        textViewFilename = view.textViewFilename
        textViewCurrentTime = view.textViewCurrentTime
        textViewMaxTime = view.textViewMaxTime
        buttonPlay = view.playButton
        buttonBack = view.prevButton
        buttonNext = view.nextButton

        mSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (mMediaPlayer != null && fromUser) {
                    mMediaPlayer!!.seekTo(progress)
                    mHandler.removeCallbacks(mRunnable)

                    val minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer!!.currentPosition.toLong())
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer!!.currentPosition.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
                    textViewCurrentTime.setText(String.format("%02d:%02d", minutes, seconds))

                    updateSeekBar()

                } else if (mMediaPlayer == null && fromUser) {
                    prepareMediaPlayerFromPoint(progress)
                    updateSeekBar()
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                if (mMediaPlayer != null) {
                    // remove message Handler from updating progress bar
                    mHandler.removeCallbacks(mRunnable)
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                if (mMediaPlayer != null) {
                    mHandler.removeCallbacks(mRunnable)
                    mMediaPlayer!!.seekTo(seekBar.progress)

                    val minutes = TimeUnit.MILLISECONDS.toMinutes(mMediaPlayer!!.currentPosition.toLong())
                    val seconds = TimeUnit.MILLISECONDS.toSeconds(mMediaPlayer!!.currentPosition.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
                    textViewCurrentTime.setText(String.format("%02d:%02d", minutes, seconds))
                    updateSeekBar()
                }
            }
        })

        buttonPlay.setOnClickListener(View.OnClickListener {
            onPlay(isPlaying)
            isPlaying = !isPlaying
        })

        textViewFilename.setText(File(filePath).nameWithoutExtension)
        textViewMaxTime.setText(String.format("%02d:%02d", minutes, seconds))

        builder.setView(view)

        // request a window without the title
        dialog.window.requestFeature(Window.FEATURE_NO_TITLE)
        return builder.create();
    }

    override fun onStart() {
        super.onStart()

        //set transparent background
        val window = dialog.window
        window!!.setBackgroundDrawableResource(android.R.color.transparent)

        //disable buttons from dialog
        val alertDialog = dialog as AlertDialog
        alertDialog.getButton(Dialog.BUTTON_POSITIVE).isEnabled = false
        alertDialog.getButton(Dialog.BUTTON_NEGATIVE).isEnabled = false
        alertDialog.getButton(Dialog.BUTTON_NEUTRAL).isEnabled = false
    }

    override fun onPause() {
        super.onPause()

        if (mMediaPlayer != null) {
            stopPlaying()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        if (mMediaPlayer != null) {
            stopPlaying()
        }
    }

    // Play start/stop
    private fun onPlay(isPlaying: Boolean) {
        if (!isPlaying) {
            //currently MediaPlayer is not playing audio
            if (mMediaPlayer == null) {
                startPlaying() //start from beginning
            } else {
                resumePlaying() //resume the currently paused MediaPlayer
            }

        } else {
            //pause the MediaPlayer
            pausePlaying()
        }
    }

    private fun startPlaying() {
        playButton.setImageResource(android.R.drawable.ic_media_pause)
        mMediaPlayer = MediaPlayer()

        try {
            mMediaPlayer!!.setDataSource(filePath)
            mMediaPlayer!!.prepare()
            mSeekBar.max = mMediaPlayer!!.duration

            mMediaPlayer!!.setOnPreparedListener { mMediaPlayer!!.start() }
        } catch (e: IOException) {
            Log.e("Hudson", "prepare() failed")
        }

        mMediaPlayer!!.setOnCompletionListener { stopPlaying() }

        updateSeekBar()

        //keep screen on while playing audio
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun prepareMediaPlayerFromPoint(progress: Int) {
        //set mediaPlayer to start from middle of the audio file

        mMediaPlayer = MediaPlayer()

        try {
            mMediaPlayer!!.setDataSource(filePath)
            mMediaPlayer!!.prepare()
            mSeekBar.max = mMediaPlayer!!.duration
            mMediaPlayer!!.seekTo(progress)

            mMediaPlayer!!.setOnCompletionListener { stopPlaying() }

        } catch (e: IOException) {
            Log.e("Hudson", "prepare() failed")
        }

        //keep screen on while playing audio
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun pausePlaying() {
        buttonPlay.setImageResource(android.R.drawable.ic_media_play)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.pause()
    }

    private fun resumePlaying() {
        buttonPlay.setImageResource(android.R.drawable.ic_media_pause)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.start()
        updateSeekBar()
    }

    private fun stopPlaying() {
        buttonPlay.setImageResource(android.R.drawable.ic_media_play)
        mHandler.removeCallbacks(mRunnable)
        mMediaPlayer!!.stop()
        mMediaPlayer!!.reset()
        mMediaPlayer!!.release()
        mMediaPlayer = null

        mSeekBar.progress = mSeekBar.max
        isPlaying = !isPlaying

        textViewCurrentTime.setText(textViewMaxTime.getText())
        mSeekBar.progress = mSeekBar.max

        //allow the screen to turn off again once audio is finished playing
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private val mRunnable = Runnable {
        if (mMediaPlayer != null) {

            val mCurrentPosition = mMediaPlayer!!.currentPosition
            mSeekBar.progress = mCurrentPosition

            val minutes = TimeUnit.MILLISECONDS.toMinutes(mCurrentPosition.toLong())
            val seconds = TimeUnit.MILLISECONDS.toSeconds(mCurrentPosition.toLong()) - TimeUnit.MINUTES.toSeconds(minutes)
            textViewCurrentTime.setText(String.format("%02d:%02d", minutes, seconds))

            updateSeekBar()
        }
    }

    private fun updateSeekBar() {
        mHandler.postDelayed(mRunnable, 1000)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param filePath Parameter 1.
         * @return A new instance of fragment PlayerFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(filePath: String) =
                PlayerFragment().apply {
                    arguments = Bundle().apply {
                        putString(ARG_filePath, filePath)
                    }
                }
    }
}
