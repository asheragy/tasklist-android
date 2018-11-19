package org.cerion.tasklist.ui;

import android.os.Bundle;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.Task;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class TaskActivity extends AppCompatActivity {
    public static final String EXTRA_TASK = "task";
    public static final String EXTRA_LIST_ID = "taskListId";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Prefs.getInstance(this).isDarkTheme())
            this.setTheme(R.style.AppTheme_Dark);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_task);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if(getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TaskFragment fragment = (TaskFragment)getSupportFragmentManager().findFragmentById(R.id.fragment);

        Task task = (Task)getIntent().getSerializableExtra(EXTRA_TASK);
        String listId = getIntent().getStringExtra(EXTRA_LIST_ID);

        if(task == null)
            fragment.showNewTask(listId);
        else
            fragment.showTask(task);

    }


}
