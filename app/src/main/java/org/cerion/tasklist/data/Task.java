package org.cerion.tasklist.data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;

public class Task implements Serializable {

    // TODO make id and listId private
    public String id;
    public String listId;

    public String title = "";
    public Date updated = new Date(0);
    public String notes = "";
    public boolean completed;
    public Date due = new Date(0);
    public boolean deleted;
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("EEE, MMM d, yyyy", Locale.US);

    private static String generateId() {
        Random rand = new Random();
        long i = rand.nextInt() + (1L << 31);
        return "temp_" + i;
    }

    public Task(String listId) {
        this(listId, generateId());
    }

    public Task(String listId, String id) {
        this.listId = listId;
        this.id = id;
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

}
