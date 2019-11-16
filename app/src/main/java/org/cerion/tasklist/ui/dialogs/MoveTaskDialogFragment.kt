package org.cerion.tasklist.ui.dialogs


import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProviders
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.data.TaskList
import org.cerion.tasklist.ui.TasksViewModel
import org.cerion.tasklist.ui.ViewModelFactory


class MoveTaskDialogFragment : DialogFragment() {

    private val viewModel: TasksViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(requireActivity(), factory).get(TasksViewModel::class.java)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = MoveTaskDialogFragmentArgs.fromBundle(requireArguments())
        val lists = viewModel.lists
        val task = viewModel.tasks.first { it.id == args.taskId }

        val adapter = ArrayAdapter<TaskList>(requireContext(), android.R.layout.simple_spinner_dropdown_item)
        adapter.addAll(lists)

        val builder = AlertDialog.Builder(activity)
                .setTitle("Move to list")
                .setAdapter(adapter) { _, which ->
                    if (args.listId.isNotEmpty())
                        viewModel.moveTaskToList(task, lists[which])
                    else
                        Log.e(TAG, "Error, unable to find task")
                }

        return builder.create()
    }
}
