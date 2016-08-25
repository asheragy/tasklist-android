package org.cerion.tasklist.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;


public class TaskActivity extends AppCompatActivity
{
    public static final String EXTRA_TASK = "task";
    public static final String EXTRA_TASKLIST = "taskList";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            this.setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task);

        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TaskFragment fragment = (TaskFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);

        Task task = (Task)getIntent().getSerializableExtra(EXTRA_TASK);
        TaskList list = (TaskList)getIntent().getSerializableExtra(EXTRA_TASKLIST);

        if(task == null)
            fragment.showNewTask(list);
        else
            fragment.showTask(task);

    }


}
