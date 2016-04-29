package org.cerion.tasklist.ui;


import android.app.Activity;
import android.app.DialogFragment;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.dialogs.DatePickerFragment;

import java.util.Date;

public class TaskFragment extends Fragment implements DatePickerFragment.DatePickerListener {

    private Task mTask;
    //private TaskList mCurrentList;
    private boolean mNewTask = false;

    private TextView mTextUpdated;
    private EditText mTitle;
    private TextView mNotes;
    private TextView mTextDue;
    private CheckBox mCheckComplete;
    private Button mRemoveDueDate;

    private MenuItem mMenuSave;

    public TaskFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_task, container, false);


        mTextUpdated = (TextView)view.findViewById(R.id.modified);
        mTitle = (EditText)view.findViewById(R.id.title);
        mNotes = (TextView)view.findViewById(R.id.notes);
        mCheckComplete = (CheckBox)view.findViewById(R.id.completed);
        mTextDue = (TextView)view.findViewById(R.id.due);
        mRemoveDueDate = (Button)view.findViewById(R.id.removeDate);

        //mTextUpdated.setVisibility(View.GONE);

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                if(s == mTitle.getEditableText() && !s.toString().contentEquals(mTask.title))
                    showSaveButton(true);
                else if(s == mNotes.getEditableText() && !s.toString().contentEquals(mTask.notes))
                    showSaveButton(true);
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
                showSaveButton(true);
            }
        });

        mRemoveDueDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTask.due.setTime(0);
                setDue();
                showSaveButton(true);
            }
        });

        //loadNewTask(mTask);

        return view;
    }


    private void onEditDueDate()
    {
        DialogFragment newFragment = DatePickerFragment.newInstance(mTask.due);
        newFragment.show(getActivity().getFragmentManager(), "datePicker");
    }

    private void showSaveButton(boolean bShow)
    {
        if(mMenuSave != null)
            mMenuSave.setVisible(bShow);
    }

    private void saveAndFinish()
    {
        mTask.setModified();
        mTask.title = mTitle.getText().toString();
        mTask.notes = mNotes.getText().toString();
        mTask.completed = mCheckComplete.isChecked();
        Database database = Database.getInstance(getContext());

        if (mNewTask)
            database.tasks.add(mTask);
        else
            database.tasks.update(mTask);

        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }


    public void showNewTask(TaskList taskList) {
        mNewTask = true;
        mTask = new Task( taskList.id );
        loadTask(mTask);
    }

    public void showTask(Task task) {
        mTask = task;
        loadTask(mTask);
    }

    private void loadTask(Task task)
    {
        if(mNewTask)
            getActivity().setTitle("Add new task");
        else
            getActivity().setTitle("Edit task");

        mTitle.setText(task.title);
        mNotes.setText(task.notes);
        Linkify.addLinks(mNotes,Linkify.ALL);

        if(task.updated != null)
            mTextUpdated.setText(task.updated.toString());
        mCheckComplete.setChecked(mTask.completed);
        setDue();

        showSaveButton(false);
    }

    private void setDue()
    {
        if(mTask.due != null && mTask.due.getTime() != 0) {
            mTextDue.setText(mTask.getDue());
            mRemoveDueDate.setVisibility(View.VISIBLE);
        }
        else {
            mTextDue.setText(getString(R.string.no_due_date));
            mRemoveDueDate.setVisibility(View.GONE);
        }
    }


    @Override
    public void onSelectDate(Date date) {
        mTask.due = date;
        setDue(); //refresh display
        showSaveButton(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.task, menu);
        mMenuSave = menu.getItem(0);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if(id == R.id.action_save)
            saveAndFinish();

        return super.onOptionsItemSelected(item);
    }
}
