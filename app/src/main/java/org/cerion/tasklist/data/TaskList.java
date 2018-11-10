package org.cerion.tasklist.data;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Random;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = TaskList.TABLE_NAME)
public class TaskList implements Serializable
{
    static final String TABLE_NAME = "tasklists";


    @PrimaryKey @NotNull
    public String id;

    @NotNull
    public String title;

    @NotNull
    private Date updated = new Date(0);

    public boolean isDefault;
    public boolean isRenamed;

    /**
     * Special list to represent a list containing tasks from all available lists
     */
    public static final TaskList ALL_TASKS = new TaskList("", "All Tasks"); //null is placeholder for "all lists"
    public boolean isAllTasks() {
        return (id.isEmpty());
    }

    @Ignore
    public TaskList(String title) {
        this(generateId(),title);
    }

    @Ignore
    public TaskList(@NotNull String id, @NotNull String title) {
        this.id = id;
        this.title = title;
    }

    @Ignore
    public TaskList(String id, String title, boolean renamed) {
        this(id,title);
        this.isRenamed = renamed;
    }

    public TaskList(@NotNull String id, @NotNull String title, @NotNull Date updated, boolean isRenamed, boolean isDefault) {
        this(id, title, isRenamed);
        this.updated = updated;
        this.isDefault = isDefault;
    }

    public String toString() {
        return title;
    }

    public static String generateId() {
        Random rand = new Random();
        long i = rand.nextInt() + (1L << 31);
        return "temp_" + i;
    }

    public boolean hasTempId() {
        return id.startsWith("temp_");
    }

    public static TaskList get(List<TaskList> lists, String sId) {
        for(TaskList list : lists) {
            if(list.isAllTasks())
                continue;

            if(list.id.contentEquals(sId))
                return list;
        }

        return null;
    }

    public static TaskList getDefault(List<TaskList> lists) {
        for(TaskList list : lists) {
            if(list.isDefault)
                return list;
        }

        return null;
    }

    @NotNull
    public Date getUpdated() {
        return updated;
    }

    public void setUpdated(@NotNull Date updated) {
        this.updated = updated;
    }
}
