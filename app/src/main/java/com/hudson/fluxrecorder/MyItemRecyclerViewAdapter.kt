package com.hudson.fluxrecorder

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaScannerConnection
import android.net.Uri
import android.support.constraint.ConstraintLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.*

class MyItemRecyclerViewAdapter(private val activity : AppCompatActivity, private val mValues : List<PlaylistFragment.Song>) : RecyclerView.Adapter<MyItemRecyclerViewAdapter.ViewHolder>() {

    init {
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder){
            item_name.text = mValues[position].file.nameWithoutExtension
            item_date.text = SimpleDateFormat("MM-dd-yyyy    HH:mm").format(Date(mValues[position].file.lastModified()))
            val uri = Uri.fromFile(mValues[position].file)
            val mmr = MediaMetadataRetriever()

            Log.d("Hudson", uri.toString())
            mmr.setDataSource(activity, uri)
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val millSecond = Integer.parseInt(durationStr)
            item_length.text = SimpleDateFormat("mm:ss").format(millSecond) + String.format("    %.2f MB", mValues[position].file.length().toFloat() / (1024*1024).toFloat())
            mView.onClick {
                var playbackFragment =
                             PlaybackFragment.newInstance(mValues.get(position));


                 playbackFragment.show(activity.supportFragmentManager, "dialog_playback");
            }
            mView.onLongClick {
                launchMenu(mValues.get(position).file, position, activity)
            }
        }
    }

    override fun getItemCount(): Int {
        Log.d("Hudson", "mValues is ${mValues.size} long")
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        var item_name : TextView
        var item_length : TextView
        var item_date : TextView
        var constraint_layout : ConstraintLayout
        var mItem: File? = null
        init {
            item_name = mView.findViewById(R.id.item_name)
            item_length = mView.findViewById(R.id.item_length)
            item_date = mView.findViewById(R.id.item_date)
            constraint_layout = mView.findViewById(R.id.constraint_layout)

        }
        override fun toString(): String {
            return super.toString() + " '" + item_name.text + "'"
        }
    }

    private fun launchMenu(file : File, index : Int, context : Context){
        var alrt2 = context.alert("Are you sure?") {
            yesButton {
                file.delete()
                MediaScannerConnection.scanFile(activity, arrayOf(file.path), null,
                    MediaScannerConnection.OnScanCompletedListener { path, uri ->
                        Log.d("Hudson", "scanned ${path}")
                        EventBus.getDefault().post(PlaylistFragment.RefreshRecyclerView());

                    })
                activity.toast("Successful toast")
            }
            noButton {  }
        }
        var alrt = context.alert("Delete file?") {
            yesButton { alrt2.show(); }
            noButton {  }
        }
        val choices = listOf("Share", "Rename", "Delete")
        context.selector(file.nameWithoutExtension, choices, { dialogInterface, i ->
            if(i == 0){

            }
            if(i == 1){
                if (!file.canWrite()) {
                    context.toast("Unable to write.")
                } else {
                    val renameDialog = RenameFileDialog.newInstance(file)
                    renameDialog.show(activity?.fragmentManager,
                            RenameFileDialog.TAG)
                }
            }
            if(i == 2){
                alrt.show()
            }
        })
    }

}
