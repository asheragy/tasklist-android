package org.cerion.tasklist.ui

import android.graphics.Paint
import android.util.TypedValue
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat
import androidx.databinding.ObservableList
import androidx.recyclerview.widget.RecyclerView
import org.cerion.tasklist.R
import org.cerion.tasklist.common.OnListAnyChangeCallback
import org.cerion.tasklist.database.Task
import org.cerion.tasklist.databinding.ListItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

interface TaskListener {
    fun open(task: Task)
    fun toggleComplete(task: Task)
    fun undelete(task: Task)
}

internal class TaskListAdapter(private val tasks: ObservableList<Task>,
                               private val taskListener: TaskListener) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>() {
    private var primaryColor: Int = 0
    private var secondaryColor: Int = 0
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
        primaryColor = ContextCompat.getColor(context, typedValue.resourceId)
        theme.resolveAttribute(android.R.attr.textColorSecondary, typedValue, true)
        secondaryColor = ContextCompat.getColor(context, typedValue.resourceId)

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
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = ListItemTaskBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = tasks[position]
        holder.bind(task)
    }

    override fun getItemCount(): Int = tasks.size

    fun getItem(position: Int): Task {
        return tasks[position]
    }

    inner class ViewHolder internal constructor(private val binding: ListItemTaskBinding) : RecyclerView.ViewHolder(binding.root), View.OnCreateContextMenuListener, OnCheckedChangeListener {

        init {
            binding.root.setOnCreateContextMenuListener(this)
            binding.completed.setOnCheckedChangeListener(this)
        }

        fun bind(task: Task) {
            binding.task = task
            binding.apply {
                listener = taskListener
                executePendingBindings()

                if (task.completed) {
                    title.setTextColor(secondaryColor)
                    title.paintFlags = title.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                } else {
                    title.setTextColor(if (task.deleted) secondaryColor else primaryColor)
                    title.paintFlags = title.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                }

                if (task.hasDueDate)
                    dueDate.text = dateFormat.format(task.due)
                else
                    dueDate.text = ""
            }
        }

        override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            itemPosition = layoutPosition
            val task = tasks[itemPosition]

            menu?.add(Menu.NONE, R.id.complete, Menu.NONE, if(!task.completed) "Complete" else "Un-Complete")
            menu?.add(Menu.NONE, R.id.delete, Menu.NONE, if(!task.deleted) "Delete" else "Un-Delete")
            menu?.add(Menu.NONE, R.id.move, Menu.NONE, "Move")
        }

        override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
            if (binding.task?.completed != isChecked) { //checkbox was manually changed

                val anim = AlphaAnimation(1.0f, 0.0f)
                anim.duration = 500
                anim.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationStart(animation: Animation) {}
                    override fun onAnimationEnd(animation: Animation) { taskListener.toggleComplete(binding.task!!) }
                    override fun onAnimationRepeat(animation: Animation) {}
                })

                this.itemView.startAnimation(anim)
            }
        }
    }
}
