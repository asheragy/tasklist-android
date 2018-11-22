package org.cerion.tasklist.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toolbar
import androidx.databinding.ObservableList
import org.cerion.tasklist.R
import org.cerion.tasklist.common.OnListAnyChangeCallback
import org.cerion.tasklist.data.TaskList
import java.util.*

class TaskListsToolbar(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : Toolbar(context, attrs, defStyleAttr) {

    private var mSpinner: Spinner
    private var mSpinnerAdapter: ArrayAdapter<TaskList>? = null
    private var vm: TasksViewModel? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.toolbar_tasklists, this)
        mSpinner = findViewById(R.id.spinner)
    }

    fun setViewModel(vm: TasksViewModel) {
        this.vm = vm

        mSpinnerAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, vm.lists)
        mSpinner.adapter = mSpinnerAdapter


        mSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            private var init = false
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (!init)
                    init = true
                else {
                    Log.d(TAG, "onNavigationItemSelected: " + position + " index = " + mSpinner.selectedItemPosition)
                    vm.setList(vm.lists[position])
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }

        vm.lists.addOnListChangedCallback(object : OnListAnyChangeCallback<ObservableList<TaskList>>() {
            override fun onAnyChange(sender: ObservableList<*>) {
                mSpinnerAdapter!!.notifyDataSetChanged()

                //Re-select last
                mSpinner.setSelection(getListPosition(Objects.requireNonNull<TaskList>(vm.currList)))
            }
        })
    }

    fun moveLeft() {
        var position = mSpinner.selectedItemPosition
        position = (position + 1) % mSpinner.count
        mSpinner.setSelection(position, true)
    }

    fun moveRight() {
        var position = mSpinner.selectedItemPosition
        position = (position - 1 + mSpinner.count) % mSpinner.count
        mSpinner.setSelection(position, true)
    }

    private fun getListPosition(list: TaskList): Int {
        val id = list.id
        var index = 0
        if (id.isNotEmpty()) {
            for (i in 1 until mSpinnerAdapter!!.count) { //Skip first since its default
                val curr = mSpinnerAdapter!!.getItem(i)
                if (curr != null && curr.id.contentEquals(id))
                    index = i
            }
        }

        Log.d(TAG, "listPosition = $index")
        return index
    }

    companion object {
        private val TAG = TaskListsToolbar::class.java.simpleName
    }
}
