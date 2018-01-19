package com.hudson.fluxrecorder

import java.io.File;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.TextView;
import android.R.string.cancel
import android.R.string.ok
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.util.Log
import org.greenrobot.eventbus.EventBus
import org.jetbrains.anko.find
import org.jetbrains.anko.longToast
import org.jetbrains.anko.toast


class RenameFileDialog : DialogFragment() {

    interface RenameFileDialogListener {
        fun onDialogPostiveClick(f: File, newName: String)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        super.onCreateDialog(savedInstanceState)
        val builder = AlertDialog.Builder(activity)
        val inflater = getActivity().layoutInflater
        val layout = inflater.inflate(R.layout.rename_dialog, null)
        val et = layout.find<EditText>(R.id.new_filename)
        val f = arguments.getSerializable(KEY_FILE) as File
        et.setText(f.nameWithoutExtension)
        val tv = layout.find<TextView>(R.id.old_filename)
        tv.text = "Rename '"+ f.nameWithoutExtension + "'"
        builder.setView(layout)
        builder.setPositiveButton("OK",
                DialogInterface.OnClickListener { dialog, id ->
                    val to = File(f.parent, et.text
                            .toString() + ".wav")
                    if (!to.name.startsWith(".") && f.renameTo(to)) {
                        //Event Bus
                        MediaScannerConnection.scanFile(activity, arrayOf(f.path, to.path), null,
                            MediaScannerConnection.OnScanCompletedListener { path, uri ->
                                Log.d("Hudson", "scanned ${path}")
                                EventBus.getDefault().post(PlaylistFragment.RefreshRecyclerView())

                            })
                        activity.runOnUiThread {
                            EventBus.getDefault().post(PlaylistFragment.RefreshRecyclerView())
                            activity.toast("Successful rename")
                        }
                    } else {
                        activity.longToast("Failed to rename")
                    }
                })
        builder.setNegativeButton("Cancel",
                DialogInterface.OnClickListener { dialog, id -> })

        val dialog = builder.create()
        dialog.window!!.setSoftInputMode(
                LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        return dialog
    }

    companion object {

        private val KEY_FILE = "file"
        val TAG = "rename_dialog"

        internal fun newInstance(f: File): RenameFileDialog {
            val d = RenameFileDialog()
            val args = Bundle()
            args.putSerializable(KEY_FILE, f)
            d.arguments = args
            return d
        }
    }
}