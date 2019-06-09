package org.cerion.tasklist.dialogs


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle

import androidx.fragment.app.DialogFragment

class AlertDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alert = AlertDialog.Builder(activity)

        val bundle = arguments
        alert.setTitle(bundle!!.getString(TITLE))
        alert.setMessage(bundle.getString(MESSAGE))
        alert.setPositiveButton("OK", null)

        return alert.create()
    }

    companion object {
        private const val TITLE = "title"
        private const val MESSAGE = "message"

        fun newInstance(sTitle: String, sMessage: String): AlertDialogFragment {
            val frag = AlertDialogFragment()

            val args = Bundle()
            args.putString(TITLE, sTitle)
            args.putString(MESSAGE, sMessage)
            frag.arguments = args

            return frag
        }
    }
}
