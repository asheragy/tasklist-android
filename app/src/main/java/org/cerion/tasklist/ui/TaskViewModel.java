package org.cerion.tasklist.ui;

import android.content.Context;
import android.databinding.Observable;
import android.databinding.ObservableField;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.Database;
import org.cerion.tasklist.data.Task;

import java.util.Date;

public class TaskViewModel {

    private Task task;
    private Context context;
    private boolean isNew = false;

    private Date dueDate;
    public ObservableField<String> title = new ObservableField<>("");
    public ObservableField<String> notes = new ObservableField<>("");
    public ObservableField<Boolean> completed = new ObservableField<>(false);
    public ObservableField<String> due = new ObservableField<>("");
    public ObservableField<Boolean> hasDueDate = new ObservableField<>(false);
    public ObservableField<Boolean> isDirty = new ObservableField<>(false);

    public TaskViewModel(Context context) {
        this.context = context;
    }

    public void addTask(String taskListId) {
        isNew = true;
        Task task = new Task( taskListId );
        setTask(task);
    }

    public void setTask(Task task) {
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

        isDirty.set(false);
    }

    public void save() {
        task.setModified();
        task.title = title.get();
        task.notes = notes.get();
        task.completed = completed.get();
        task.due = dueDate != null ? dueDate : new Date(0);

        Database database = Database.getInstance(context);
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
            due.set(context.getString(R.string.no_due_date));
            hasDueDate.set(false);
        }
    }
}
