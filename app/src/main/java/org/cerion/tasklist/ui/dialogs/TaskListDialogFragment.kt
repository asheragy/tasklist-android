package org.cerion.tasklist.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import androidx.fragment.app.DialogFragment
import org.cerion.tasklist.database.AppDatabase
import org.cerion.tasklist.database.TaskList
import org.cerion.tasklist.database.getById
import java.util.*

class TaskListDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val alert = AlertDialog.Builder(activity)
        val edittext = EditText(activity)
        alert.setTitle("Task List Name")
        alert.setView(edittext)

        val bundle = arguments
        val type = bundle!!.getInt(TYPE)
        if (type == TYPE_ADD) {

            alert.setPositiveButton("Save") { _, _ ->
                val name = edittext.text.toString()

                val db = AppDatabase.getInstance(requireContext())!!.taskListDao()
                val update = TaskList(AppDatabase.generateTempId(), name)
                db.add(update)

                // TODO not sure if this method is prefered or onActivityResult is better
                if (targetFragment is TaskListsChangedListener)
                    (targetFragment as TaskListsChangedListener).onTaskListsChanged(update)
            }
        } else if (type == TYPE_RENAME) {
            val listId = bundle.getString(LISTID)
            val listName = bundle.getString(LISTNAME)
            edittext.setText(listName)

            alert.setPositiveButton("Save") { _, _ ->
                val newName = edittext.text.toString()
                Log.d(TAG, "Rename $listName to $newName")

                val db = AppDatabase.getInstance(requireActivity())!!.taskListDao()
                val list = db.getAll().getById(listId!!)!!
                list.title = newName
                list.updated = Date()
                db.update(list)

                val parent = targetFragment as TaskListsChangedListener
                parent.onTaskListsChanged(list)
            }
        }

        alert.setNegativeButton("Cancel") { _, _ -> Log.d(TAG, "onCancel") }

        return alert.create()
    }

    companion object {

        private val TAG = TaskListDialogFragment::class.java.simpleName
        private const val TYPE = "type"
        private const val LISTID = "id"
        private const val LISTNAME = "name"

        private const val TYPE_ADD = 0
        private const val TYPE_RENAME = 1

        fun getAddInstance(): TaskListDialogFragment {
            val frag = TaskListDialogFragment()

            val args = Bundle()
            args.putInt(TYPE, TYPE_ADD)
            frag.arguments = args

            return frag
        }

        fun getRenameInstance(list: TaskList): TaskListDialogFragment {
            val frag = TaskListDialogFragment()
            val args = Bundle()
            args.putInt(TYPE, TYPE_RENAME)
            args.putString(LISTID, list.id)
            args.putString(LISTNAME, list.title)
            frag.arguments = args

            return frag
        }
    }
}
