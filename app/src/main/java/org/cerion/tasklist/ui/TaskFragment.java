package org.cerion.tasklist.ui;


import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.databinding.Observable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.databinding.FragmentTaskBinding;
import org.cerion.tasklist.dialogs.DatePickerFragment;

import java.util.Date;

public class TaskFragment extends Fragment implements DatePickerFragment.DatePickerListener {

    private static final String TAG = TaskFragment.class.getSimpleName();
    private FragmentTaskBinding binding;
    private MenuItem mMenuSave;
    private TaskViewModel vm;

    public TaskFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        vm = new TaskViewModel(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTaskBinding.inflate(inflater, container, false);
        binding.setViewModel(vm);

        vm.isDirty.addOnPropertyChangedCallback(new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                if(mMenuSave != null)
                    mMenuSave.setVisible(vm.isDirty.get());
            }
        });

        // Must be on view, not the binded field
        binding.notes.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void afterTextChanged(Editable editable) {
                Linkify.addLinks(binding.notes,Linkify.ALL);
            }
        });

        binding.due.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onEditDueDate();
            }
        });

        return binding.getRoot();
    }

    private void onEditDueDate() {
        DialogFragment newFragment = DatePickerFragment.newInstance(vm.getDueDate());
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getActivity().getFragmentManager(), "datePicker");
    }

    private void saveAndFinish() {
        vm.save();

        getActivity().setResult(Activity.RESULT_OK);
        getActivity().finish();
    }

    public void showNewTask(TaskList taskList) {
        getActivity().setTitle("Add new task");
        vm.addTask(taskList.id);
    }

    public void showTask(Task task) {
        getActivity().setTitle("Edit task");
        vm.setTask(task);

        // TODO move to viewmodel
        if(task.updated != null)
            binding.modified.setText(task.updated.toString());
    }

    @Override
    public void onSelectDate(Date date) {
        vm.setDue(date);
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
