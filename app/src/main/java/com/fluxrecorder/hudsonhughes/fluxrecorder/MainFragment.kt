package com.fluxrecorder.hudsonhughes.fluxrecorder

import android.content.Context
import android.net.Uri
import android.opengl.Visibility
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.progressDialog
import com.fluxrecorder.hudsonhughes.fluxrecorder.R
import org.jetbrains.anko.*
import java.io.File
import java.io.RandomAccessFile
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Future
import kotlin.concurrent.thread

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [MainFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class MainFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }
    var buttons : ArrayList<Button> = java.util.ArrayList<Button>(240)
    var buttonTimes : ArrayList<Int> = java.util.ArrayList<Int>(240)
    fun saveAudio(requestedSeconds : Int) : Future<Unit> {
        var dialog = progressDialog(message = "Please wait a bit…", title = "Saving wav file")
        var cancel = false
        var target = File(App.storagePath).resolve(getCurrentLocalDateTimeStamp() + ".wav")
        var task = doAsync( {throwable : Throwable -> target.delete(); dialog.dismiss(); alert("Failed to save file.")} ) {
            uiThread { dialog.show() }
            var headerArray = ByteArray(44)
            target.appendBytes(headerArray)
            val requestedBytes = App.bytesPerSecond * requestedSeconds
            var leftToProcess = requestedBytes.toLong()
            var processed = 0L
            val newestToOldest = App.instance.folder.listFiles().sortedByDescending { file -> file.name }
            var mapPointers: MutableMap<File, Long> = mutableMapOf<File, Long>()
            for (f: File in newestToOldest) {
                if (cancel) return@doAsync
                if (leftToProcess - f.length() > -1) {
                    mapPointers.put(f, 0L)
                    leftToProcess -= f.length()
                    processed += f.length()
                    Log.d("Hudson", "Processed ${f.name} ${0L}")
                } else {
                    mapPointers.put(f, f.length() - leftToProcess)
                    processed += f.length() - leftToProcess
                    Log.d("Hudson", "Processed ${f.name} ${f.length() + (leftToProcess - f.length())}")
                    break
                }
            }
            Log.d("Hudson", "Map to process" + mapPointers.toString())
            var processedBytes = 0
            var progress = 0
            var previousProgress = 0
            if (cancel) return@doAsync
            for (f: File in App.instance.folder.listFiles().sortedBy { file -> file.name }) {
                if (mapPointers.containsKey(f)) {
                    val location = mapPointers[f]
                    if (location != null) {
                        Log.d("Hudson", "Processing ${f.name} starting at ${location}")
                        var pointer = RandomAccessFile(f, "r")
                        pointer.skipBytes(location.toInt())
                        var barray = ByteArray(1024)
                        var amountTaken = pointer.read(barray)
                        processedBytes += amountTaken
                        while (amountTaken > 0) {
                            if (cancel) return@doAsync
                            target.appendBytes(barray)
                            amountTaken = pointer.read(barray)
                            processedBytes += amountTaken
                            var holder = (processedBytes.toFloat() / requestedBytes.toFloat() * 100.toFloat()).toInt()
                            if (holder != previousProgress) {
                                previousProgress = progress
                                progress = holder
                                uiThread { dialog.progress = progress }
                            }
                        }
                    }
                }
                var wavHeader = WavHeader(RandomAccessFile(target, "rw"), 44100, 1, 16)
                wavHeader.writeHeader()
            }
            uiThread { dialog.dismiss() }
        }
        dialog.setOnCancelListener {
            task.cancel(true)
            cancel = true
            target.delete()
        }
        return task
    }
    var dots = 0
    lateinit var rootView : View
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_main, container, false)

        every(750, {
                if (App.isServiceRunning()) {
                    rootView.recordingTextView.text = "Recording now" + (".".repeat(dots))
                    if (++dots > 3) dots = 0
                    rootView.recordingTextView.visibility = View.VISIBLE
                } else {



                    rootView.recordingTextView.text = ""
                    rootView.recordingTextView.visibility = View.GONE
                }
                if (!App.instance.canHandleBufferDuration(App.secondsDesired.toInt())) {
                    errorTextView.text = "There is not enough space for the buffer to occupy."
                    //.format(App.instance.requiredSpace(App.secondsDesired)/1024))
                    rootView.recordingTextView.visibility = View.VISIBLE
                } else {
                    errorTextView.text = ""
                    rootView.recordingTextView.visibility = View.GONE
                }
                rootView.statusTextView.text = String.format(getString(R.string.state_of_recorder), "", "")
                val bufferDuration = App.bufferDuration
                if (bufferDuration < 1) {
                    rootView.saveTextView.text = getString(R.string.empty_buffer)
                } else {
                    rootView.saveTextView.text = getString(R.string.how_much)
                }
                buttons.forEachIndexed { index, button ->
                    if (bufferDuration > 60 * (index + 1)) {
                        button.visibility = View.VISIBLE
                        button.text = formatTime(60 * (index + 1))
                        buttonTimes[index] = 60 * (index + 1)
                        button.setOnClickListener {
                            // Handler code here.
                            val requestedTime = buttonTimes[index]
                            if (!App.instance.canHandleBufferDuration(requestedTime))
                                alert("There is not enough space to store a file this large.").show()
                            else
                                saveAudio(requestedTime)
                        }
                    } else if (bufferDuration < 60 * (index + 1) && bufferDuration - 60 * (index + 1) < 60) {
                        button.visibility = View.VISIBLE
                        button.text = formatTime(bufferDuration)
                        buttonTimes[index] = bufferDuration
                        button.setOnClickListener {
                            // Handler code here.
                            val requestedTime = buttonTimes[index]
                            if (!App.instance.canHandleBufferDuration(requestedTime))
                                alert("There is not enough space to store a file this large.").show()
                            else
                                saveAudio(requestedTime)
                        }
                    } else
                        button.visibility = View.GONE
                    button.setOnClickListener { }
                }
        })
        var dateFormat : DateFormat = SimpleDateFormat("mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))
        for((index, item) in buttons.withIndex()){
            item.visibility = View.GONE
            item.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            scrollLayout.addView(item)
        }
        return rootView
    }

    fun getCurrentLocalDateTimeStamp(): String {
        return SimpleDateFormat("dd-MM-yyyy_hh:mm:ss").format(Date())
    }

    public fun formatTime(time : Long) : String{
        return SimpleDateFormat("mm:ss", Locale.getDefault()).format(Date(time * 1000))
    }

    public fun formatTime(time : Int) : String{
        return SimpleDateFormat("mm:ss", Locale.getDefault()).format(Date(time.toLong() * 1000))
    }

    fun every(interval: Long, func: () -> Unit) {
        thread {
            while (true) {
                Thread.sleep(interval)
                context?.runOnUiThread{
                    func()
                }
            }
        }
    }
    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment MainFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() : Fragment {
            return MainFragment()
        }
    }
}
