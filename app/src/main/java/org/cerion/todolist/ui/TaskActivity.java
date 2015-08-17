package org.cerion.todolist.ui;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import org.cerion.todolist.data.Database;
import org.cerion.todolist.R;
import org.cerion.todolist.data.Task;
import org.cerion.todolist.data.TaskList;
import org.cerion.todolist.dialogs.DatePickerFragment;

import java.util.Date;


public class TaskActivity extends ActionBarActivity implements DatePickerFragment.DatePickerListener
{
    public static final String EXTRA_TASK = "task";
    public static final String EXTRA_TASKLIST = "taskList";
    public static final String EXTRA_DEFAULT_LIST = "defaultList";

    private Task mTask;
    private TaskList mCurrentList;
    private boolean mNewTask = false;

    private View mEditButtons;
    private TextView mTextTaskId;
    private TextView mTextUpdated;
    private EditText mTitle;
    private TextView mNotes;
    private TextView mTextDue;
    private CheckBox mCheckComplete;
    private Button mRemoveDueDate;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_task);

        mTask = (Task)getIntent().getSerializableExtra(EXTRA_TASK);
        mCurrentList = (TaskList)getIntent().getSerializableExtra(EXTRA_TASKLIST);
        TaskList defaultList = (TaskList)getIntent().getSerializableExtra(EXTRA_DEFAULT_LIST);

        if(mTask == null) {
            mNewTask = true;
            //If current list is "All Tasks" then add new to default list
            mTask = new Task( mCurrentList.id == null ? defaultList.id : mCurrentList.id );
        }

        mTextTaskId = (TextView)findViewById(R.id.taskid);
        mTextUpdated = (TextView)findViewById(R.id.modified);
        mTitle = (EditText)findViewById(R.id.title);
        mNotes = (TextView)findViewById(R.id.notes);
        mCheckComplete = (CheckBox)findViewById(R.id.completed);
        mTextDue = (TextView)findViewById(R.id.due);
        mEditButtons = findViewById(R.id.edit_buttons);
        mRemoveDueDate = (Button)findViewById(R.id.removeDate);

        mTextTaskId.setVisibility(View.GONE);
        //mTextUpdated.setVisibility(View.GONE);


        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                Log.d("TAG","onTextChanged");
            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s == mTitle.getEditableText() && !s.toString().contentEquals(mTask.title))
                    setEditMode(true);
                else if(s == mNotes.getEditableText() && !s.toString().contentEquals(mTask.notes))
                    setEditMode(true);
            }
        };

        mTitle.addTextChangedListener(watcher);
        mNotes.addTextChangedListener(watcher);

        mTextDue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditDueDate();
            }
        });

        mCheckComplete.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setEditMode(true);
            }
        });

        mRemoveDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask.due.setTime(0);
                setDue();
                setEditMode(true);
            }
        });

        findViewById(R.id.saveButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinish(true);
            }
        });

        findViewById(R.id.cancelButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onFinish(false);
            }
        });

        loadTask(mTask);
    }

    private void onEditDueDate()
    {
        DialogFragment newFragment = DatePickerFragment.newInstance(mTask.due);
        newFragment.show(getFragmentManager(), "datePicker");
    }

    private void setEditMode(boolean bEdit)
    {
        if(bEdit)
            mEditButtons.setVisibility(View.VISIBLE);
        else
            mEditButtons.setVisibility(View.GONE);

    }

    private void onFinish(boolean bSave)
    {
        if(bSave) {
            mTask.setModified();
            mTask.title = mTitle.getText().toString();
            mTask.notes = mNotes.getText().toString();
            mTask.completed = mCheckComplete.isChecked();
            Database database = Database.getInstance(this);

            if (mNewTask)
                database.tasks.add(mTask);
            else
                database.tasks.update(mTask);

            setResult(RESULT_OK);
        }
        else
            setResult(RESULT_CANCELED);

        finish();
    }

    private void loadTask(Task task)
    {
        TaskList parent = mCurrentList;//TaskList.get(MainActivity.mTaskLists,task.listId);
        setTitle(parent.title);

        mTextTaskId.setText(task.id);
        mTitle.setText(task.title);

        if(task.updated != null)
            mTextUpdated.setText(task.updated.toString());

        mNotes.setText(task.notes);

        mCheckComplete.setChecked(mTask.completed);

        setDue();

        if(mTask.deleted)
            findViewById(R.id.deleted).setVisibility(View.VISIBLE);

        setEditMode(false);
    }

    private void setDue()
    {
        if(mTask.due != null && mTask.due.getTime() != 0) {
            mTextDue.setText(mTask.getDue());
            mRemoveDueDate.setVisibility(View.VISIBLE);
        }
        else {
            mTextDue.setText("No Due Date");
            mRemoveDueDate.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSelectDate(Date date) {
        mTask.due = date;
        setDue(); //refresh display
        setEditMode(true);
    }
}
