package org.cerion.tasklist.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.cerion.tasklist.R
import org.cerion.tasklist.data.Prefs
import org.cerion.tasklist.data.Task


class TaskDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Prefs.getInstance(this).isDarkTheme)
            this.setTheme(R.style.AppTheme_Dark)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_task)

        val id = intent.getStringExtra(TaskDetailFragment.EXTRA_TASK_ID)
        val listId = intent.getStringExtra(TaskDetailFragment.EXTRA_LIST_ID)

        //val fragment = supportFragmentManager.findFragmentById(R.id.fragment) as TaskDetailFragment
        val fragment = TaskDetailFragment.getInstance(listId, id)

        supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragment, fragment)
                .commit()

    }

    companion object {
        fun getIntent(context: Context, listId: String, task: Task?): Intent {
            val intent = Intent(context, TaskDetailActivity::class.java)
            intent.putExtra(TaskDetailFragment.EXTRA_LIST_ID, listId)
            intent.putExtra(TaskDetailFragment.EXTRA_TASK_ID, task?.id ?: "")

            return intent
        }
    }


}
