package org.cerion.tasklist.data;

import android.util.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class TaskList implements Serializable
{
    public String id;
    public String title;
    private Date updated;
    public boolean bDefault = false;
    private int renamed;

    public TaskList(String title)
    {
        this(generateId(),title);
    }

    public TaskList(String id, String title)
    {
        this.id = id;
        this.title = title;
        renamed = -1;
    }

    public TaskList(String id, String title, boolean renamed)
    {
        this(id,title);
        this.renamed = (renamed ? 1 : 0);
    }

    public String toString()
    {
        return title;
    }

    public void clearRenamed()
    {
        if(renamed == -1)
            Log.e("TaskList","renamed undefined in TaskList");

        renamed = 0;
    }


    public boolean hasRenamed()
    {
        return (renamed >= 0);
    }

    public boolean isRenamed()
    {
        if(renamed == -1)
            Log.e("TaskList","renamed undefined in TaskList");

        return (renamed == 1);
    }

    public static String generateId()
    {
        Random rand = new Random();
        long i = rand.nextInt() + (1L << 31);
        return "temp_" + i;
    }

    public boolean hasTempId()
    {
        return id.startsWith("temp_");
    }

    public static TaskList get(ArrayList<TaskList> lists, String sId) {
        for(TaskList list : lists) {
            if(list.id == null)
                continue;

            if(list.id.contentEquals(sId))
                return list;
        }

        return null;
    }

    public static TaskList getDefault(ArrayList<TaskList> lists) {
        for(TaskList list : lists) {
            if(list.bDefault)
                return list;
        }

        return null;
    }

    public Date getUpdated()
    {
        if(updated == null)
            return new Date(0);

        return updated;
    }

    public void setUpdated(Date updated)
    {
        this.updated = updated;
    }


}
