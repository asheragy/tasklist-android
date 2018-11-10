package org.cerion.tasklist.ui;

import android.app.Application;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;

import java.util.Date;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

public class TaskViewModel extends AndroidViewModel {

    private Task task;
    private boolean isNew = false;
    private Date dueDate;

    public ObservableField<String> windowTitle = new ObservableField<>("");
    public ObservableField<String> title = new ObservableField<>("");
    public ObservableField<String> notes = new ObservableField<>("");
    public ObservableBoolean completed = new ObservableBoolean(false);
    public ObservableBoolean hasDueDate = new ObservableBoolean(false);
    public ObservableField<String> due = new ObservableField<>("");
    public ObservableField<Boolean> isDirty = new ObservableField<>(false);
    public ObservableField<String> modified = new ObservableField<>("");

    public TaskViewModel(@NonNull Application application) {
        super(application);
    }

    public void addTask(String taskListId) {
        if (!isNew) {
            isNew = true;
            Task task = new Task(taskListId);
            windowTitle.set("Add new task");
            loadTaskFields(task);
        }
    }

    public void setTask(Task task) {
        if (this.task == null || !this.task.id.contentEquals(task.id)) {
            windowTitle.set("Edit task");
            loadTaskFields(task);
        }
    }

    private void loadTaskFields(Task task) {
        this.task = task;

        title.set(task.title);
        notes.set(task.notes);
        completed.set(task.completed);
        setDue(task.due);

        // TODO this seems to be detecting the set() from above when it should not
        // If that can be fixed the notifyChange() inside can be removed
        Observable.OnPropertyChangedCallback onPropertyChangedCallback = new Observable.OnPropertyChangedCallback() {
            @Override
            public void onPropertyChanged(Observable observable, int i) {
                isDirty.set(true);
                isDirty.notifyChange();
            }
        };

        title.addOnPropertyChangedCallback(onPropertyChangedCallback);
        notes.addOnPropertyChangedCallback(onPropertyChangedCallback);
        completed.addOnPropertyChangedCallback(onPropertyChangedCallback);
        due.addOnPropertyChangedCallback(onPropertyChangedCallback);

        if(task.updated != null && task.updated.getTime() > 0)
            modified.set(task.updated.toString());
        else
            modified.set("");

        isDirty.set(false);
    }

    public void save() {
        task.setModified();
        task.title = title.get();
        task.notes = notes.get();
        task.completed = completed.get();
        task.due = dueDate != null ? dueDate : new Date(0);

        Database database = Database.getInstance(getApplication());
        if (isNew)
            database.tasks.add(task);
        else
            database.tasks.update(task);

        isNew = false;
    }

    public void removeDueDate() {
        setDue(null);
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDue(Date date) {
        dueDate = date;

        if(date != null && date.getTime() != 0) {

            Task temp = new Task("");
            temp.due = date;
            due.set(temp.getDue());
            hasDueDate.set(true);
        }
        else {
            due.set(getApplication().getString(R.string.no_due_date));
            hasDueDate.set(false);
        }
    }
}
