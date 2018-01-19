package com.hudson.fluxrecorder

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.android.synthetic.main.fragment_main.view.*
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.progressDialog
import org.jetbrains.anko.*
import java.io.File
import java.io.RandomAccessFile
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import android.content.Intent
import android.net.Uri
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.act
import com.ikovac.timepickerwithseconds.MyTimePickerDialog
import com.ikovac.timepickerwithseconds.TimePicker
import org.jetbrains.anko.support.v4.longToast


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
    fun saveAudio(requestedSeconds : Int) {
        var dialog = progressDialog(message = "Please wait a bit…", title = "Saving wav file")
        var cancel = false
        if(!App.instance.canHandleWavFileDuration(requestedSeconds)){
            longToast("Save unsuccessful. Insufficient space.")
            return
        }else {
            var target = File(App.storagePath).resolve(getCurrentLocalDateTimeStamp() + ".wav")
            var task = doAsync({ throwable: Throwable -> target.delete(); dialog.dismiss(); alert("Failed to save file.") }) {
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

                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = Uri.fromFile(target)
                    context!!.sendBroadcast(intent)
                }
                uiThread { dialog.dismiss() }
            }
            dialog.setOnCancelListener {
                task.cancel(true)
                cancel = true
                target.delete()
            }
        }
//        return task
    }
    var dots = 0
    lateinit var rootView : View

    private fun updateUI(){
        if(isAdded){
            var instruction_string = getString(R.string.instruction_off)
            if (App.isServiceRunning()) {
                instruction_string = getString(R.string.instruction_on)
                rootView.recordingTextView.text = "Recording now" + (".".repeat(dots))
                if (++dots > 3) dots = 0
                rootView.recordingTextView.visibility = View.VISIBLE
                rootView.instructions.text = getString(R.string.instruction_on)
            } else {
                rootView.recordingTextView.text = ""
                rootView.recordingTextView.visibility = View.GONE
            }
            rootView.instructions.text = instruction_string
            if (!App.instance.canHandleBufferDuration(App.secondsDesired.toInt())) {
                rootView.errorTextView.text = "There is not enough space for the buffer to occupy."
                //.format(App.instance.requiredSpace(App.secondsDesired)/1024))
                rootView.errorTextView.visibility = View.VISIBLE
            } else {
                rootView.errorTextView.text = ""
                rootView.errorTextView.visibility = View.GONE
            }

            val buffer_minutes_max = App.secondsDesired / 60
            val buffer_seconds_max = App.secondsDesired % 60

            var buff_duration = App.bufferDuration
            if(buff_duration > App.secondsDesired) buff_duration = App.secondsDesired

            var buffer_minutes = buff_duration / 60
            var buffer_seconds = buff_duration % 60
//            if(buffer_minutes_max < buffer_minutes) buffer_minutes = buffer_minutes_max
//            if(buffer_seconds_max < buffer_seconds) buffer_seconds = buffer_seconds_max

            rootView.statusTextView.text = String.format(getString(R.string.state_of_recorder), String.format("%d:%02d", buffer_minutes, buffer_seconds), String.format("%d:%02d", buffer_minutes_max, buffer_seconds_max))
            var bufferDuration = App.bufferDuration
            if(bufferDuration > App.secondsDesired) bufferDuration = App.secondsDesired
            if (bufferDuration < 1) {
                rootView.saveTextView.text = getString(R.string.empty_buffer)
            } else {
                rootView.saveTextView.text = getString(R.string.how_much)
            }

            Log.d("Hudson", "Cycle")

        }
    }

    lateinit var UIObservable : Disposable

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        rootView = inflater.inflate(R.layout.fragment_main, container, false)

        var dateFormat : DateFormat = SimpleDateFormat("mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"))

        rootView.save_button.onClick {
            if(App.bufferDuration < 1){
                activity?.toast("There is nothing in the buffer to save.")
            }else{
                val mTimePicker = MyTimePickerDialog(getContext(), object : MyTimePickerDialog.OnTimeSetListener {

                    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int, seconds: Int) {
                        // TODO Auto-generated method stub
                        /*time.setText(getString(R.string.time) + String.format("%02d", hourOfDay)+
						":" + String.format("%02d", minute) +
						":" + String.format("%02d", seconds));	*/
                        var requested_time : Int = hourOfDay * 60 * 60 + minute * 60 + seconds * 60
                        if(requested_time > (if (App.bufferDuration > App.secondsDesired)  App.secondsDesired else App.bufferDuration)){
                            alert(message = "The time specified is larger than the buffer.", title = "Save entire buffer?"){
                                positiveButton("Yes"){ saveAudio(requested_time) }
                                negativeButton("Cancel"){}
                            }.show()
                        }else{
                            saveAudio(requested_time)
                        }
                    }
                }, 0, 1, 0, true)
                mTimePicker.setTitle("How much do you want?")
                mTimePicker.show()
            }
        }

        updateUI()
        UIObservable = Observable.interval(500L, TimeUnit.MILLISECONDS)
            .observeOn( AndroidSchedulers.mainThread() )
            .subscribe { updateUI() }



        return rootView
    }

    override fun onPause() {
        super.onPause()
        if(UIObservable != null && !UIObservable.isDisposed){
            UIObservable.dispose()
        }
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

    companion object {
        @JvmStatic
        fun newInstance() : Fragment {
            return MainFragment()
        }
    }
}
