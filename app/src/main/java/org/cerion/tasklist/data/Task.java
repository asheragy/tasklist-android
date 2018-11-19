package org.cerion.tasklist.data;

import org.jetbrains.annotations.NotNull;

import java.util.Date;

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
public class Task {

    static final String TABLE_NAME = "tasks";

    @NotNull private String id;
    @NotNull private String listId;
    @NotNull private String title;
    @NotNull private Date due;
    @NotNull private Date updated;
    @NotNull private String notes;
    private boolean completed;
    private boolean deleted;

    @Ignore
    public Task(String listId) {
        this(listId, AppDatabase.generateTempId());
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

    public @NotNull String getId() {
        return id;
    }

    public void setId(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String getListId() {
        return listId;
    }

    public void setListId(@NotNull String listId) {
        this.listId = listId;
    }

    public @NotNull String getTitle() {
        return title;
    }

    public void setTitle(@NotNull String title) {
        this.title = title;
    }

    public @NotNull String getNotes() {
        return notes;
    }

    public void setNotes(@NotNull String notes) {
        this.notes = notes;
    }

    public void setModified() {
        updated = new Date();
    }

    public @NotNull Date getDue() {
        return due;
    }

    public void setDue(@NotNull Date due) {
        this.due = due;
    }

    public boolean hasDueDate() {
        return due.getTime() > 0;
    }

    public boolean getCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public boolean getDeleted() { return deleted; }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public @NotNull Date getUpdated() {
        return updated;
    }

    public void setUpdated(@NotNull Date updated) {
        this.updated = updated;
    }

    public boolean hasTempId() {
        return id.startsWith("temp_");
    }

    public boolean isBlank() {
        return (title.length() == 0 && notes.length() == 0 && due.getTime() == 0);
    }
}
