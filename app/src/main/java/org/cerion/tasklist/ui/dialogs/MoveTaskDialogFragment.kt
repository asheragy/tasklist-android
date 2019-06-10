package org.cerion.tasklist.ui.dialogs


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.Task
import org.cerion.tasklist.data.TaskList

class MoveTaskDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val adapter = ArrayAdapter<TaskList>(activity!!, android.R.layout.simple_spinner_dropdown_item)

        val db = AppDatabase.getInstance(requireContext())!!
        val taskDb = db.taskDao()
        val lists = db.taskListDao().getAll()

        adapter.addAll(lists)

        val bundle = arguments!!
        val taskId = bundle.getString(TASK_ID)!!
        val listId = bundle.getString(TASK_LISTID)!!
        val task = taskDb.get(listId, taskId)

        val builder = AlertDialog.Builder(activity)
        builder.setTitle("Move to list")
                .setAdapter(adapter) { _, which ->
                    if (listId.isNotEmpty()) {
                        val list = lists[which]
                        if (list.id.contentEquals(listId)) {
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
                            (activity as TaskListsChangedListener).onTaskListsChanged(list)
                        }

                    } else {
                        Log.e(TAG, "Error, unable to find task")
                    }
                }

        return builder.create()
    }

    companion object {
        private const val TASK_LISTID = "taskListId"
        private const val TASK_ID = "taskId"
        private val TAG = MoveTaskDialogFragment::class.java.simpleName

        fun newInstance(task: Task): MoveTaskDialogFragment {
            val frag = MoveTaskDialogFragment()

            val args = Bundle()
            args.putString(TASK_LISTID, task.listId)
            args.putString(TASK_ID, task.id)

            frag.arguments = args
            return frag
        }
    }
}
