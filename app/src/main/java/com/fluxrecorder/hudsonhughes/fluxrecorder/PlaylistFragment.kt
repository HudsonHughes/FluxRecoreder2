package com.fluxrecorder.hudsonhughes.fluxrecorder

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import java.io.File
import android.provider.MediaStore
import android.text.SpannableStringBuilder
import android.util.Log
import org.jetbrains.anko.*
import java.io.IOException
import java.time.Duration
import android.provider.Settings.System.canWrite
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.toast


class PlaylistFragment : ListFragment() {
    // TODO: Customize parameters
    private var mColumnCount = 1
    private lateinit var adapter : MyItemRecyclerViewAdapter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        var view = inflater.inflate(R.layout.fragment_item_list, container, false)

        // Set the adapter
        if (view is RecyclerView) {
            val context = view.getContext()
            if (mColumnCount <= 1) {
                view.layoutManager = LinearLayoutManager(context)
            } else {
                view.layoutManager = GridLayoutManager(context, mColumnCount)
            }
            view.adapter = MyItemRecyclerViewAdapter(act, getMusic())
        }
        return view
    }

    private fun getMusic(): List<Song> {
        var songFiles = mutableListOf<Song>()
        var selection = "is_music != 0"

//        if (albumId > 0) {
//            selection = selection + " and album_id = " + albumId
//        }

        val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID)
        val sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC"

        var cursor: Cursor? = null
        try {
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            cursor = activity!!.contentResolver.query(uri, projection, selection, null, sortOrder)
            if (cursor != null) {
                cursor!!.moveToFirst()
                val position = 1
                while (!cursor!!.isAfterLast()) {
                    songFiles.add(Song(File(cursor!!.getString(2)), cursor!!.getLong(4)))
                    cursor!!.moveToNext()
                }
            }

        } catch (e: Exception) {
            Log.e("Hudson Hughes", e.toString())
        } finally {
            if (cursor != null) {
                cursor!!.close()
            }
        }

        return songFiles;
    }

    companion object {
        // TODO: Customize parameter initialization
        fun newInstance(): PlaylistFragment {
            val fragment = PlaylistFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }

    class Song(val file : File, val duration : Long){
    }



    fun isFilenameValid(file: String, folder : File): Boolean {
        val f = folder.resolve(file)
        if(f.exists()) return false
        try {
            f.canonicalPath
            return true
        } catch (e: IOException) {
            return false
        }

    }
}
