package org.cerion.tasklist.ui


import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.android.material.datepicker.MaterialDatePicker
import org.cerion.tasklist.R
import org.cerion.tasklist.databinding.FragmentTaskBinding
import java.util.*

class TaskDetailFragment : Fragment() {

    private lateinit var binding: FragmentTaskBinding

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
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)

        viewModel.windowTitle.observe(viewLifecycleOwner, Observer { title -> (requireActivity() as AppCompatActivity).supportActionBar?.title = title })

        // Must be on view, not the binded field
        binding.notes.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
            override fun afterTextChanged(editable: Editable) {
                Linkify.addLinks(binding.notes, Linkify.ALL)
            }
        })

        binding.due.setOnClickListener { onEditDueDate() }

        if (arguments != null) {
            val args = TaskDetailFragmentArgs.fromBundle(arguments!!)
            if (args.taskId.isNullOrEmpty())
                viewModel.addTask(args.listId)
            else
                viewModel.setTask(args.listId, args.taskId)
        }

        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.task_detail, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> saveAndFinish()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onEditDueDate() {
        val builder = MaterialDatePicker.Builder.datePicker()
        if (viewModel.task.value!!.dueDate.value.time != 0L)
            builder.setSelection(viewModel.task.value!!.dueDate.value.time)

        val picker = builder.build()
        picker.addOnPositiveButtonClickListener {
            viewModel.task.value!!.dueDate.value = Date(it)
        }

        picker.show(parentFragmentManager, picker.toString())
    }

    private fun saveAndFinish() {
        val inputManager = requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusedView = requireActivity().currentFocus
        if (currentFocusedView != null)
            inputManager.hideSoftInputFromWindow(currentFocusedView.windowToken, HIDE_NOT_ALWAYS)

        val changes = viewModel.save()
        tasksViewModel.hasLocalChanges.set(changes)
        requireActivity().onBackPressed()
    }
}
