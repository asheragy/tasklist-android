package org.cerion.tasklist.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.Task


class TaskDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            this.setTheme(R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as TaskDetailFragment

        val id = intent.getStringExtra(EXTRA_TASK_ID)
        val listId = intent.getStringExtra(EXTRA_LIST_ID)

        if (id.isEmpty())
            fragment.showNewTask(listId)
        else
            fragment.showTask(listId, id)

    }

    companion object {
        private const val EXTRA_TASK_ID = "taskId"
        private const val EXTRA_LIST_ID = "taskListId"

        fun getIntent(context: Context, listId: String, task: Task?): Intent {
            val intent = Intent(context, TaskDetailActivity::class.java)
            intent.putExtra(TaskDetailActivity.EXTRA_LIST_ID, listId)
            intent.putExtra(TaskDetailActivity.EXTRA_TASK_ID, task?.id ?: "")

            return intent
        }
    }


}
