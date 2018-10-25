package org.cerion.tasklist.ui;

import android.content.Context;
import android.databinding.ObservableList;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toolbar;

import org.cerion.tasklist.R;
import org.cerion.tasklist.common.OnListAnyChangeCallback;
import org.cerion.tasklist.data.TaskList;

import java.util.Objects;

public class TaskListsToolbar extends Toolbar {

    private static final String TAG = TaskListsToolbar.class.getSimpleName();

    //private final List<TaskList> mTaskLists = new ArrayList<>();
    private Spinner mSpinner;
    private ArrayAdapter<TaskList> mSpinnerAdapter;
    private TasksViewModel vm;

    public void setViewModel(final TasksViewModel vm) {
        this.vm = vm;

        mSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, vm.getLists());
        mSpinner.setAdapter(mSpinnerAdapter);

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onNavigationItemSelected: " + position + " index = " + mSpinner.getSelectedItemPosition());
                vm.setList( vm.getLists().get(position) );
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        vm.getLists().addOnListChangedCallback(new OnListAnyChangeCallback<ObservableList<TaskList>>() {
            @Override
            public void onAnyChange(ObservableList sender) {
                mSpinnerAdapter.notifyDataSetChanged();

                //Re-select last
                mSpinner.setSelection(getListPosition(Objects.requireNonNull(vm.getCurrList())));
            }
        });
    }

    public TaskListsToolbar(Context context) {
        super(context);
        init(context);
    }

    public TaskListsToolbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TaskListsToolbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.toolbar_tasklists, this);
        mSpinner = findViewById(R.id.spinner);
    }

    public void moveLeft() {
        int position = mSpinner.getSelectedItemPosition();
        position = (position + 1) % mSpinner.getCount();
        mSpinner.setSelection(position, true);
    }

    public void moveRight() {
        int position = mSpinner.getSelectedItemPosition();
        position = (position - 1 + mSpinner.getCount()) % mSpinner.getCount();
        mSpinner.setSelection(position, true);
    }

    private int getListPosition(TaskList list) {
        String id = list.id;
        int index = 0;
        if (id != null) {
            for (int i = 1; i < mSpinnerAdapter.getCount(); i++) { //Skip first since its default
                TaskList curr = mSpinnerAdapter.getItem(i);
                if (curr.id.contentEquals(id))
                    index = i;
            }
        }

        Log.d(TAG, "listPosition = " + index);
        return index;
    }
}
