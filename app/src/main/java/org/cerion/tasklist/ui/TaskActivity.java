package org.cerion.tasklist.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;


public class TaskActivity extends AppCompatActivity {
    private static final String EXTRA_TASK_ID = "taskId";
    private static final String EXTRA_LIST_ID = "taskListId";

    public static Intent getIntent(Context context, @NotNull String listId, @Nullable Task task) {
        Intent intent = new Intent(context, TaskActivity.class);
        intent.putExtra(TaskActivity.EXTRA_LIST_ID, listId);
        intent.putExtra(TaskActivity.EXTRA_TASK_ID, task != null ? task.getId() : "");

        return intent;
    }

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

        String id = getIntent().getStringExtra(EXTRA_TASK_ID);
        String listId = getIntent().getStringExtra(EXTRA_LIST_ID);

        if(id.isEmpty())
            fragment.showNewTask(listId);
        else
            fragment.showTask(listId, id);

    }


}
