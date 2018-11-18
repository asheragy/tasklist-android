package org.cerion.tasklist.data;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;

@Entity(tableName = Task.TABLE_NAME,
        primaryKeys = {"id","listId"},
        foreignKeys = @ForeignKey(
                entity = TaskList.class,
                parentColumns = "id",
                childColumns = "listId",
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE))
public class Task implements Serializable {

    static final String TABLE_NAME = "tasks";

    // TODO make id and listId private

    @NotNull public String id;
    @NotNull public String listId;
    @NotNull public String title;
    @NotNull public Date due;
    @NotNull public Date updated;
    @NotNull public String notes;
    public boolean completed;
    public boolean deleted;

    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

    @Ignore
    public Task(String listId) {
        this(listId, generateId());
    }

    @Ignore
    public Task(String listId, String id) {
        this(listId, id, "", "", new Date(0), new Date(0), false, false);
    }

    public Task(@NotNull String listId, @NotNull String id, @NotNull String title, @NotNull String notes, @NotNull Date due, @NotNull Date updated, boolean completed, boolean deleted) {
        this.listId = listId;
        this.id = id;
        this.title = title;
        this.notes = notes;
        this.due = due;
        this.updated = updated;
        this.completed = completed;
        this.deleted = deleted;
    }

    public String toString() {
        return title + (deleted ? " (Deleted)" : "");
    }

    public void setDeleted(boolean del) {
        deleted = del;
        setModified();
    }

    public void setCompleted(boolean complete) {
        completed = complete;
        setModified();
    }

    public void setModified() {
        updated = new Date();
    }

    public boolean hasTempId() {
        return id.startsWith("temp_");
    }

    public String getDue() {
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return mDateFormat.format(due);
    }

    public boolean isBlank() {
        return (title.length() == 0 && notes.length() == 0 && due.getTime() == 0);
    }

    public void moveToList(String listId) {
        this.listId = listId;
        this.id = generateId();
    }

    public static Task getTask(List<Task> tasks, String id) {
        Task task = null;

        for(Task t : tasks) {
            if (t.id.contentEquals(id))
                task = t;
        }

        return task;
    }

    private static String generateId() {
        Random rand = new Random();
        long i = rand.nextInt() + (1L << 31);
        return "temp_" + i;
    }
}
