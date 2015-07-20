package org.cerion.todolist;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;


public class TaskActivity extends ActionBarActivity
{
    public static final String EXTRA_TASK_ID = "taskId";
    //private String mTaskId;
    public static Task mTask; //TODO, read from database or something better

    private View mEditButtons;
    private TextView mTextTaskId;
    private TextView mTextList;
    private TextView mTextUpdated;
    private EditText mTitle;
    private TextView mTextNotes;
    private TextView mTextDue;
    private CheckBox mCheckComplete;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        //mTaskId = getIntent().getStringExtra("taskId");

        mTextTaskId = (TextView)findViewById(R.id.taskid);
        mTextList = (TextView)findViewById(R.id.list);
        mTextUpdated = (TextView)findViewById(R.id.modified);
        mTitle = (EditText)findViewById(R.id.title);
        mTextNotes = (TextView)findViewById(R.id.notes);
        mCheckComplete = (CheckBox)findViewById(R.id.completed);
        mTextDue = (TextView)findViewById(R.id.due);
        mEditButtons = findViewById(R.id.edit_buttons);


        mTitle.addTextChangedListener(new TextWatcher()
        {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after)
            {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count)
            {

            }

            @Override
            public void afterTextChanged(Editable s)
            {
                if(!s.toString().contentEquals(mTask.title))
                    setEditMode(true);
            }
        });


        loadTask(mTask);
    }

    public void setEditMode(boolean bEdit)
    {
        if(bEdit)
            mEditButtons.setVisibility(View.VISIBLE);
        else
            mEditButtons.setVisibility(View.GONE);

    }

    public void loadTask(Task task)
    {
        TaskList parent = TaskList.get(MainActivity.mTaskLists,task.listId);


        mTextList.setText(parent.title);
        mTextTaskId.setText("TASKID: " + task.id);
        mTextUpdated.setText(mTask.updated.toString());
        mTitle.setText(task.title);
        mTextNotes.setText(task.notes);

        mCheckComplete.setChecked(mTask.completed);

        if(mTask.due != null)
            mTextDue.setText(mTask.due.toString());
        else
            mTextDue.setText("No Due Date");

        if(mTask.deleted)
            findViewById(R.id.deleted).setVisibility(View.VISIBLE);

        setEditMode(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_task, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
