package org.cerion.tasklist.ui;

import android.app.Application;

import org.cerion.tasklist.R;
import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.Task;
import org.cerion.tasklist.data.TaskDao;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;
import androidx.databinding.Observable;
import androidx.databinding.ObservableBoolean;
import androidx.databinding.ObservableField;
import androidx.lifecycle.AndroidViewModel;

public class TaskViewModel extends AndroidViewModel {

    // TODO remove double due fields and use binding converter for dateFormat
    // https://mlsdev.com/blog/57-android-data-binding

    private Task task;
    private boolean isNew = false;
    private @NotNull Date dueDate = new Date(0);
    private TaskDao db;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

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
        db = AppDatabase.getInstance(getApplication()).taskDao();
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void addTask(@NotNull String taskListId) {
        if (!isNew) {
            isNew = true;
            Task task = new Task(taskListId);
            windowTitle.set("Add new task");
            loadTaskFields(task);
        }
    }

    public void setTask(String listId, String id) {
        if (this.task == null || !this.task.getId().contentEquals(task.getId())) {
            windowTitle.set("Edit task");

            Task task = db.get(listId, id);
            loadTaskFields(task);
            loadTaskFields(task);
        }
    }

    private void loadTaskFields(Task task) {
        this.task = task;

        title.set(task.getTitle());
        notes.set(task.getNotes());
        completed.set(task.getCompleted());
        setDue(task.getDue());

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

        // TODO is it ever this value?
        if(task.getUpdated().getTime() > 0)
            modified.set(task.getUpdated().toString());
        else
            modified.set("");

        isDirty.set(false);
    }

    public void save() {
        task.setModified();
        task.setTitle(title.get());
        task.setNotes(notes.get());
        task.setCompleted(completed.get());
        task.setDue(dueDate);

        if (isNew)
            db.add(task);
        else
            db.update(task);

        isNew = false;
    }

    public void removeDueDate() {
        setDue(new Date(0));
    }

    public Date getDueDate() {
        return dueDate;
    }

    public void setDue(@NotNull Date date) {
        dueDate = date;

        if(date.getTime() != 0) {
            due.set( dateFormat.format(date));
            hasDueDate.set(true);
        }
        else {
            due.set(getApplication().getString(R.string.no_due_date));
            hasDueDate.set(false);
        }
    }
}
