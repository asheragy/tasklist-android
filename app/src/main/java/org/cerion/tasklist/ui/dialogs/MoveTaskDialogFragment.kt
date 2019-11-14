package org.cerion.tasklist.ui.dialogs


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.TaskList
import java.util.*


class MoveTaskDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val adapter = ArrayAdapter<TaskList>(activity!!, android.R.layout.simple_spinner_dropdown_item)
        val db = AppDatabase.getInstance(requireContext())!!
        val taskDb = db.taskDao()
        val lists = db.taskListDao().getAll().sortedBy { it.title.toLowerCase(Locale.getDefault()) }

        adapter.addAll(lists)

        val args = MoveTaskDialogFragmentArgs.fromBundle(arguments!!)
        val task = taskDb.get(args.listId, args.taskId)

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Move to list")
                .setAdapter(adapter) { _, which ->
                    if (args.listId.isNotEmpty()) {
                        val list = lists[which]
                        if (list.id.contentEquals(args.listId)) {
                            Log.d(TAG, "Ignoring moving since same list")
                        } else {
                            // Delete task, add to new list and refresh
                            // TODO if temp ID permanently delete
                            task.setModified()
                            task.deleted = true
                            taskDb.update(task)

                            task.deleted = false
                            task.listId = list.id
                            task.id = AppDatabase.generateTempId()
                            taskDb.add(task)

                            // TODO would be good to have test for this
                            val parent = targetFragment as TaskListsChangedListener
                            parent.onTaskListsChanged(list)
                        }

                    } else {
                        Log.e(TAG, "Error, unable to find task")
                    }
                }

        return builder.create()
    }
}
