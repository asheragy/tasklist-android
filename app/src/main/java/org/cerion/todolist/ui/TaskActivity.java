package org.cerion.todolist.ui;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import org.cerion.todolist.Database;
import org.cerion.todolist.R;
import org.cerion.todolist.Task;
import org.cerion.todolist.TaskList;
import org.cerion.todolist.ui.MainActivity;

import java.util.Date;


public class TaskActivity extends ActionBarActivity
{
    public static final String EXTRA_TASK = "task";
    public static final String EXTRA_TASKLIST = "taskList";
    public static final String EXTRA_DEFAULT_LIST = "defaultList";

    private Task mTask;
    private TaskList mCurrentList;
    private TaskList mDefaultList;
    private boolean mNewTask = false;

    private View mEditButtons;
    private TextView mTextTaskId;
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

        //TODO, pass in current list + position
        mTask = (Task)getIntent().getSerializableExtra(EXTRA_TASK);
        mCurrentList = (TaskList)getIntent().getSerializableExtra(EXTRA_TASKLIST);
        mDefaultList = (TaskList)getIntent().getSerializableExtra(EXTRA_DEFAULT_LIST);

        if(mTask == null) {
            mNewTask = true;
            //If current list is "All Tasks" then add new to default list
            mTask = new Task( mCurrentList.id == null ? mDefaultList.id : mCurrentList.id );
        }

        mTextTaskId = (TextView)findViewById(R.id.taskid);
        mTextUpdated = (TextView)findViewById(R.id.modified);
        mTitle = (EditText)findViewById(R.id.title);
        mTextNotes = (TextView)findViewById(R.id.notes);
        mCheckComplete = (CheckBox)findViewById(R.id.completed);
        mTextDue = (TextView)findViewById(R.id.due);
        mEditButtons = findViewById(R.id.edit_buttons);

        mTextTaskId.setVisibility(View.GONE);
        //mTextUpdated.setVisibility(View.GONE);

        mTitle.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().contentEquals(mTask.title)) {
                    setEditMode(true);
                }
            }
        });

        mCheckComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setEditMode(true);
            }
        });

        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveTask();
            }
        });

        findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //loadTask(mTask);
                setResult(RESULT_CANCELED);
                finish();
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

    public void saveTask()
    {
        mTask.setModified();
        mTask.title = mTitle.getText().toString();
        mTask.completed = mCheckComplete.isChecked();
        Database database = Database.getInstance(this);

        if(mNewTask)
            database.addTask(mTask);
        else
            database.updateTask(mTask);

        setResult(RESULT_OK);
        finish();

    }

    public void loadTask(Task task)
    {
        TaskList parent = mCurrentList;//TaskList.get(MainActivity.mTaskLists,task.listId);
        setTitle(parent.title);

        mTextTaskId.setText(task.id);
        mTitle.setText(task.title);

        if(task.updated != null)
            mTextUpdated.setText(task.updated.toString());

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
