package org.cerion.tasklist.ui;

import android.content.Context;
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
import org.cerion.tasklist.data.TaskList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TaskListsToolbar extends Toolbar {

    private static final String TAG = TaskListsToolbar.class.getSimpleName();

    //private final List<TaskList> mTaskLists = new ArrayList<>();
    private Spinner mSpinner;
    private ArrayAdapter<TaskList> mSpinnerAdapter;
    private TaskListsChangeListener mListener;
    private TasksViewModel vm;

    public interface TaskListsChangeListener {
        //TaskList getCurrentList();
        //void setCurrentList(TaskList list);
        void onListChanged();
    }

    public void setViewModel(final TasksViewModel vm) {
        this.vm = vm;

        mSpinnerAdapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, vm.lists);
        mSpinner.setAdapter(mSpinnerAdapter);

        mListener = (TaskListsChangeListener)getContext();

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "onNavigationItemSelected: " + position + " index = " + mSpinner.getSelectedItemPosition());
                vm.currList.set( vm.lists.get(position) );
                mListener.onListChanged();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

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
        mSpinner = (Spinner)findViewById(R.id.spinner);
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

    public void refresh(List<TaskList> lists) {
        vm.lists.clear();
        vm.lists.addAll(lists);

        Collections.sort(vm.lists, new Comparator<TaskList>() {
            @Override
            public int compare(TaskList taskList, TaskList t1) {
                return taskList.title.compareToIgnoreCase(t1.title);
            }
        });

        vm.lists.add(0, TaskList.ALL_TASKS);
        mSpinnerAdapter.notifyDataSetChanged();

        //Re-select last position
        mSpinner.setSelection(getListPosition( vm.currList.get()));
    }

    public TaskList getDefaultList() {
        return TaskList.getDefault(vm.lists);
    }
}
