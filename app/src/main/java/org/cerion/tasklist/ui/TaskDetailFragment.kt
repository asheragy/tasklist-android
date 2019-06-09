package org.cerion.tasklist.ui


import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import org.cerion.tasklist.R
import org.cerion.tasklist.databinding.FragmentTaskBinding
import org.cerion.tasklist.dialogs.DatePickerFragment
import java.util.*


class TaskDetailFragment : Fragment(), DatePickerFragment.DatePickerListener {

    companion object {
        private val TAG = TaskDetailFragment::class.java.simpleName

        const val EXTRA_TASK_ID = "taskId"
        const val EXTRA_LIST_ID = "taskListId"

        fun getBundle(listId: String, id: String): Bundle {
            val args = Bundle()
            args.putString(EXTRA_LIST_ID, listId)
            args.putString(EXTRA_TASK_ID, id)
            return args
        }
    }

    private lateinit var binding: FragmentTaskBinding
    private var menuSave: MenuItem? = null
    private val viewModel: TaskDetailViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(this, factory).get(TaskDetailViewModel::class.java)
    }

    private val tasksViewModel: TasksViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(requireActivity(), factory).get(TasksViewModel::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        val toolbar = binding.toolbar
        toolbar.inflateMenu(R.menu.task)
        menuSave = toolbar.menu.getItem(0)
        toolbar.setNavigationOnClickListener { requireActivity().onBackPressed() }
        toolbar.setOnMenuItemClickListener { item ->
            if (item?.itemId == R.id.action_save)
                saveAndFinish()

            false
        }

        viewModel.isDirty.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, i: Int) {
                if (menuSave != null)
                    menuSave!!.isVisible = viewModel.isDirty.get()!!
            }
        })

        // Must be on view, not the binded field
        binding.notes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                Linkify.addLinks(binding.notes, Linkify.ALL)
            }
        })

        binding.due.setOnClickListener { onEditDueDate() }

        val id = arguments?.getString(EXTRA_TASK_ID)
        val listId: String = arguments?.getString(EXTRA_LIST_ID)!!

        if (id!!.isEmpty())
            viewModel.addTask(listId)
        else
            viewModel.setTask(listId, id)

        return binding.root
    }

    private fun onEditDueDate() {
        val newFragment = DatePickerFragment.newInstance(viewModel.dueDate)
        newFragment.setTargetFragment(this, 0)
        newFragment.show(requireFragmentManager(), "datePicker")
    }

    private fun saveAndFinish() {
        viewModel.save()
        tasksViewModel.hasLocalChanges.set(true)
        requireActivity().onBackPressed()
    }

    override fun onSelectDate(date: Date) {
        viewModel.setDue(date)
    }
}
