package org.cerion.tasklist.ui

import android.graphics.Paint
import android.util.Log
import android.util.TypedValue
import android.view.*
import android.view.View.OnClickListener
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import org.cerion.tasklist.R
import org.cerion.tasklist.common.OnListAnyChangeCallback
import org.cerion.tasklist.database.Task
import java.text.SimpleDateFormat
import java.util.*

interface TaskListener {
    fun open(task: Task)
    fun toggleComplete(task: Task)
    fun undelete(task: Task)
}

internal class TaskListAdapter(private val tasks: ObservableList<Task>,
                               private val taskListener: TaskListener) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>() {
    private var mPrimaryColor: Int = 0
    private var mSecondaryColor: Int = 0
    private var parent: RecyclerView? = null
    private var emptyView: View? = null
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)

    //Workaround for activity to get context menu position
    var itemPosition: Int = 0
        private set

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun setEmptyView(recyclerView: RecyclerView, emptyView: View) {
        this.parent = recyclerView
        this.emptyView = emptyView
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)

        val context = recyclerView.context
        //Programmatically set color based on completion, need to know current theme
        val typedValue = TypedValue()
        val theme = context.theme
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
        mPrimaryColor = ContextCompat.getColor(context, typedValue.resourceId)
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        mSecondaryColor = ContextCompat.getColor(context, typedValue.resourceId)

        tasks.addOnListChangedCallback(object : OnListAnyChangeCallback<ObservableList<Task>>() {
            override fun onAnyChange(sender: ObservableList<*>) {
                setVisibility()
                notifyDataSetChanged()
            }
        })

        setVisibility()
    }

    private fun setVisibility() {
        if (tasks.size > 0) {
            parent?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        } else {
            parent?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.list_item_task, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]

        holder.itemView.setOnClickListener { taskListener.open(task) }

        val sTitle = if (!task.title.isBlank()) task.title else "<Blank>"
        holder.title.text = sTitle
        holder.notes.text = task.notes
        holder.completed.isChecked = task.completed

        if (task.completed) {
            holder.title.setTextColor(mSecondaryColor)
            holder.title.paintFlags = holder.title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

        } else {
            holder.title.setTextColor(if (task.deleted) mSecondaryColor else mPrimaryColor)
            holder.title.paintFlags = holder.title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        //If deleted don't show completed checkbox
        holder.completed.visibility = if (task.deleted) View.GONE else View.VISIBLE
        holder.undelete.visibility = if (task.deleted) View.VISIBLE else View.GONE

        if (task.hasDueDate)
            holder.due.text = dateFormat.format(task.due)
        else
            holder.due.text = ""
    }

    override fun getItemCount(): Int = tasks.size

    fun getItem(position: Int): Task {
        return tasks[position]
    }

    inner class ViewHolder internal constructor(view: View) : RecyclerView.ViewHolder(view), View.OnCreateContextMenuListener, OnCheckedChangeListener {
        internal val title: TextView = view.findViewById(R.id.title)
        internal val due: TextView = view.findViewById(R.id.dueDate)
        internal val notes: TextView = view.findViewById(R.id.notes)
        internal val completed: CheckBox = view.findViewById(R.id.completed)
        internal val undelete: ImageButton = view.findViewById(R.id.undelete)

        private val mOnUnDeleteListener = OnClickListener {
            val task = tasks[layoutPosition]
            taskListener.undelete(task)
        }

        init {
            view.setOnCreateContextMenuListener(this)
            completed.setOnCheckedChangeListener(this)
            undelete.setOnClickListener(mOnUnDeleteListener)
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            itemPosition = layoutPosition
            val task = tasks[itemPosition]

            menu?.add(Menu.NONE, R.id.complete, Menu.NONE, if(!task.completed) "Complete" else "Un-Complete")
            menu?.add(Menu.NONE, R.id.delete, Menu.NONE, if(!task.deleted) "Delete" else "Un-Delete")
            menu?.add(Menu.NONE, R.id.move, Menu.NONE, "Move")
        }

        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            val task = tasks[layoutPosition]
            if (task.completed != isChecked) { //checkbox was manually changed

                val anim = AlphaAnimation(1.0f, 0.0f)
                anim.duration = 500
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {
                    }

                    override fun onAnimationEnd(animation: Animation) {
                        //Update record in database and refresh list
                        Log.d(TAG, "Toggle completed checkbox")
                        taskListener.toggleComplete(task)
                    }

                    override fun onAnimationRepeat(animation: Animation) {
                    }
                })

                this.itemView.startAnimation(anim)
            }
        }
    }

    companion object {
        private val TAG = TaskListAdapter::class.java.simpleName
    }
}
