package org.cerion.tasklist.ui


import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.util.Linkify
import android.view.*
import androidx.databinding.Observable
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import org.cerion.tasklist.R
import org.cerion.tasklist.databinding.FragmentTaskBinding
import org.cerion.tasklist.dialogs.DatePickerFragment
import java.util.*

class TaskDetailFragment : Fragment(), DatePickerFragment.DatePickerListener {
    private lateinit var binding: FragmentTaskBinding
    private var menuSave: MenuItem? = null
    private val viewModel: TaskDetailViewModel by lazy {
        val factory = ViewModelFactory(requireActivity().application)
        ViewModelProviders.of(this, factory).get(TaskDetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentTaskBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel

        viewModel.isDirty.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, i: Int) {
                if (menuSave != null)
                    menuSave!!.isVisible = viewModel.isDirty.get()!!
            }
        })

        viewModel.windowTitle.addOnPropertyChangedCallback(object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(observable: Observable, i: Int) {
                requireActivity().title = viewModel.windowTitle.get()
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
        return binding.root
    }

    private fun onEditDueDate() {
        val newFragment = DatePickerFragment.newInstance(viewModel.dueDate)
        newFragment.setTargetFragment(this, 0)
        newFragment.show(activity!!.supportFragmentManager, "datePicker")
    }

    private fun saveAndFinish() {
        viewModel.save()

        activity?.setResult(Activity.RESULT_OK)
        activity?.finish()
    }

    fun showNewTask(listId: String) {
        viewModel.addTask(listId)
    }

    fun showTask(listId: String, id: String) {
        viewModel.setTask(listId, id)
    }

    override fun onSelectDate(date: Date) {
        viewModel.setDue(date)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater!!.inflate(R.menu.task, menu)
        menuSave = menu!!.getItem(0)
        menuSave!!.isVisible = viewModel.isDirty.get()!!
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == R.id.action_save)
            saveAndFinish()

        return super.onOptionsItemSelected(item)
    }

    companion object {
        private val TAG = TaskDetailFragment::class.java.simpleName
    }
}
