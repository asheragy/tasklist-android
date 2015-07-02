package org.cerion.andy.todolist;

import java.util.Date;
import java.util.Random;

public class Task
{
    public String id;
    public String listId;
    public String title;
    public Date updated;
    public String notes = "";
    public boolean completed;
    public Date due;
    public boolean deleted;


    public Task(String listId, String id)
    {
        this.listId = listId;
        this.id = id;
    }

    public String toString()
    {
        return title + (deleted ? " (Deleted)" : "");
    }

    public void setDeleted(boolean del)
    {
        deleted = del;
        setModified();
    }

    public void setModified()
    {
        updated = new Date();
    }

    public static String generateId()
    {
        Random rand = new Random();
        int i = rand.nextInt();
        return "temp_" + i;
    }

    public boolean hasTempId()
    {
        return id.startsWith("temp_");
    }
}
