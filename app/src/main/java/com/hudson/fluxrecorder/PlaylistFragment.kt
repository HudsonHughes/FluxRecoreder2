package com.hudson.fluxrecorder

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
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
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.DividerItemDecoration
import kotlinx.android.synthetic.main.fragment_item_list.view.*
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.sdk25.coroutines.onLongClick
import org.jetbrains.anko.support.v4.toast
import org.greenrobot.eventbus.ThreadMode
import org.greenrobot.eventbus.Subscribe
import org.jetbrains.anko.support.v4.act
import org.jetbrains.anko.support.v4.onRefresh


class PlaylistFragment : Fragment() {
    // TODO: Customize parameters
    private var mColumnCount = 1
    private lateinit var adapter : MyItemRecyclerViewAdapter
    private lateinit var view2 : View
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        view2 = inflater.inflate(R.layout.fragment_item_list, container, false)
        // Set the adapter
        var listView = view2.list



        var empt_view = view2.empty_view
        if (listView is RecyclerView) {
            var context = listView.getContext()
            if (mColumnCount <= 1) {
                listView.layoutManager = LinearLayoutManager(context)
            } else {
                listView.layoutManager = GridLayoutManager(context, mColumnCount)
            }

            var dividerItemDecoration = DividerItemDecoration(listView.context,
                    listView.layoutManager.layoutDirection);


            val ls = getMusic()
            view2.list.adapter = MyItemRecyclerViewAdapter(context as AppCompatActivity, ls)
            if (ls.isEmpty()){
                view2.list.setVisibility(View.GONE);
                view2.empty_view.setVisibility(View.VISIBLE);
            } else {
                view2.list.setVisibility(View.VISIBLE);
                view2.empty_view.setVisibility(View.GONE);
            }
        }
        view2.header.text = "Looking in ${App.storagePath}\nLong click for more options. Pull down to refresh."
        view2.swiperefresh.onRefresh {
            val ls = getMusic()
            view2.list.adapter = MyItemRecyclerViewAdapter(context as AppCompatActivity, ls)
            if (ls.isEmpty()){
                view2.list.setVisibility(View.GONE);
                view2.empty_view.setVisibility(View.VISIBLE);
            } else {
                view2.list.setVisibility(View.VISIBLE);
                view2.empty_view.setVisibility(View.GONE);
            }
            view2.swiperefresh.isRefreshing = false
        }
        return view2
    }
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(event: RefreshRecyclerView) {
        val ls = getMusic()
        view2.list.adapter = MyItemRecyclerViewAdapter(context as AppCompatActivity, ls)
        if (ls.isEmpty()){
            view2.list.setVisibility(View.GONE);
            view2.empty_view.setVisibility(View.VISIBLE);
        } else {
            view2.list.setVisibility(View.VISIBLE);
            view2.empty_view.setVisibility(View.GONE);
        }
        view2.swiperefresh.isRefreshing = false
    };
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this);
    }
    override fun onStop() {
        super.onStop()
        EventBus.getDefault().unregister(this);
    }
    private fun getMusic(): List<Song> {

        File(App.storagePath).listFiles().map {
            if(it.extension == "wav"){
                Log.d("Hudson", it.path)
                val intent2 = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                intent2.data = Uri.fromFile(it)
                context?.sendBroadcast(intent2)
            }
        }

        var songFiles = mutableListOf<Song>()
        var selection = "is_music != 0"

        val projection = arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM, MediaStore.Audio.Media.ALBUM_ID)
        val sortOrder = MediaStore.Audio.AudioColumns.TITLE + " COLLATE LOCALIZED ASC"

        var cursor: Cursor? = null
        try {
            val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
            Log.d("Hudson", App.storagePath)
            cursor = activity!!.contentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection,MediaStore.Audio.Media.DATA + " like ? ",
                    arrayOf<String>("%fluxrecorder%"),  null);
            if (cursor != null) {
                cursor!!.moveToFirst()
                while (!cursor!!.isAfterLast()) {
                    Log.d("Hudson", "Found: ${cursor!!.getString(2)}")
                    if(File(cursor!!.getString(2)).exists())
                        songFiles.add(Song(File(cursor!!.getString(2)), cursor!!.getLong(4)))
                    else{
                        val rootUri : Uri = MediaStore.Audio.Media.getContentUriForPath( File(cursor!!.getString(2)).absolutePath );  // Change file types here
                        activity!!.contentResolver.delete( rootUri, MediaStore.MediaColumns.DATA + "=?", arrayOf( File(cursor!!.getString(2)).absolutePath ) );
                    }
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

//        File(App.storagePath).list().filter { it.endsWith(".wav", true) }
        for(i in songFiles){
            Log.d("Hudson", i.file.path + " " + i.duration)
        }
        songFiles.sortBy { -it.file.lastModified() }
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
    class Song(val file : File, val duration : Long){}
    class RefreshRecyclerView(){}
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
