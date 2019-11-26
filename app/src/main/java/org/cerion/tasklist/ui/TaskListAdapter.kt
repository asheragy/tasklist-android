package org.cerion.tasklist.ui

import android.graphics.Paint
import android.util.TypedValue
import android.view.*
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.CompoundButton
import android.widget.CompoundButton.OnCheckedChangeListener
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.cerion.tasklist.R
import org.cerion.tasklist.database.Task
import org.cerion.tasklist.databinding.ListItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

interface TaskListener {
    fun open(task: Task)
    fun toggleComplete(task: Task)
    fun toggleDeleted(task: Task)
    fun undelete(task: Task)
    fun move(task: Task)
}

internal class TaskListAdapter(private val taskListener: TaskListener) : RecyclerView.Adapter<TaskListAdapter.ViewHolder>() {

    private var primaryColor: Int = 0
    private var secondaryColor: Int = 0
    private var parent: RecyclerView? = null
    private var emptyView: View? = null
    private val dateFormat = SimpleDateFormat("EEE, MMM d, yyyy", Locale.US)
    private var tasks: List<Task> = emptyList()

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    fun setEmptyView(recyclerView: RecyclerView, emptyView: View) {
        this.parent = recyclerView
        this.emptyView = emptyView
    }

    fun setTasks(tasks: List<Task>) {
        this.tasks = tasks
        setVisibility()
        notifyDataSetChanged()
    }

    private fun setVisibility() {
        if (tasks.isNotEmpty()) {
            parent?.visibility = View.VISIBLE
            emptyView?.visibility = View.GONE
        } else {
            parent?.visibility = View.GONE
            emptyView?.visibility = View.VISIBLE
        }
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
            val task = binding.task!!

            menu?.apply {
                add(Menu.NONE, R.id.complete, Menu.NONE, if(!task.completed) "Complete" else "Un-Complete").setOnMenuItemClickListener {
                    taskListener.toggleComplete(task)
                    true
                }
                add(Menu.NONE, R.id.delete, Menu.NONE, if(!task.deleted) "Delete" else "Un-Delete").setOnMenuItemClickListener {
                    taskListener.toggleDeleted(task)
                    true
                }
                add(Menu.NONE, R.id.move, Menu.NONE, "Move").setOnMenuItemClickListener {
                    taskListener.move(task)
                    true
                }
            }
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
