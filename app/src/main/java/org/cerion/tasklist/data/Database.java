package org.cerion.tasklist.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Database extends DBBase
{
    private static final String TAG = Database.class.getSimpleName();

    public final TaskLists taskLists;
    public final Tasks tasks;

    //Singleton class
    private static Database mInstance;
    private Database(Context context)
    {
        super(DatabaseOpenHelper.getInstance(context));
        taskLists = new TaskLists(this);
        tasks = new Tasks(this);
    }

    public synchronized static Database getInstance(Context context)
    {
        if(mInstance == null)
            mInstance = new Database(context.getApplicationContext());

        return mInstance;
    }

    public static class TaskLists extends DatabaseOpenHelper.TaskLists
    {
        private final Database parent;
        TaskLists(Database db)
        {
            parent = db;
        }

        private static TaskList get(Cursor c)
        {
            String title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE));
            String id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID));
            int renamed = c.getInt(c.getColumnIndexOrThrow(COLUMN_RENAMED));
            int def = c.getInt(c.getColumnIndexOrThrow(COLUMN_DEFAULT));

            TaskList result = new TaskList(id,title, (renamed == 1) );
            result.isDefault = (def == 1);
            result.setUpdated ( new Date( c.getLong(c.getColumnIndexOrThrow(COLUMN_UPDATED)) ));
            return result;
        }

        public List<TaskList> getList()
        {
            SQLiteDatabase db = parent.openReadOnly();

            Cursor c = db.query(TABLE_NAME, null, null, null, null, null, null);
            ArrayList<TaskList> result = new ArrayList<>();

            if(c != null)
            {
                while (c.moveToNext())
                {
                    TaskList list = TaskLists.get(c);
                    result.add(list);
                }
                c.close();
            }

            db.close();
            return result;
        }

        public void add(TaskList taskList)
        {
            Log.d(TAG,"addTaskList: " + taskList.title);
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, taskList.id);
            values.put(COLUMN_TITLE, taskList.title);
            values.put(COLUMN_DEFAULT, taskList.isDefault);

            parent.insert(TABLE_NAME, values);
        }

        public void update(TaskList taskList)
        {
            String where = COLUMN_ID + "='" + taskList.id + "'";
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, taskList.title);
            values.put(COLUMN_DEFAULT, taskList.isDefault);
            values.put(COLUMN_RENAMED, (taskList.isRenamed ? 1 : 0) );

            parent.update(TABLE_NAME, values, where);
        }

        public void delete(TaskList taskList)
        {
            SQLiteDatabase db = parent.open();
            db.beginTransaction();

            Log.d(TAG, "deleteTaskList: " + taskList.title);
            //First delete tasks to not violate foreign key constraints
            parent.delete(db, Tasks.TABLE_NAME, String.format("%s='%s'", Tasks.COLUMN_LISTID, taskList.id) );
            parent.delete(db, TABLE_NAME, String.format("%s='%s'", COLUMN_ID, taskList.id) );

            db.setTransactionSuccessful();
            db.endTransaction();
            db.close();
        }

        //This is only set after tasks have successfully synced, so it needs to be updated on its own
        public void setLastUpdated(TaskList taskList, Date updated)
        {
            String where = COLUMN_ID + "='" + taskList.id + "'";
            ContentValues values = new ContentValues();
            values.put(COLUMN_UPDATED, updated.getTime());

            parent.update(TABLE_NAME, values, where);
        }

        public void setId(TaskList taskList, String sNewId)
        {
            String where = String.format("%s='%s'", COLUMN_ID, taskList.id);
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, sNewId);
            parent.update(TABLE_NAME, values, where);
            //Foreign key ON UPDATE CASCADE will update associated tasks
        }

    }

    public static class Tasks extends DatabaseOpenHelper.Tasks
    {
        private final Database parent;
        Tasks(Database db)
        {
            parent = db;
        }

        private static Task get(Cursor c)
        {
            String list = c.getString(c.getColumnIndexOrThrow(COLUMN_LISTID));
            String id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID));
            Task task = new Task(list,id);
            task.title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE));
            task.notes = c.getString(c.getColumnIndexOrThrow(COLUMN_NOTES));
            task.completed = (c.getInt(c.getColumnIndexOrThrow(COLUMN_COMPLETE)) == 1);
            task.deleted = (c.getInt(c.getColumnIndexOrThrow(COLUMN_DELETED)) == 1);

            long updated = c.getLong(c.getColumnIndexOrThrow(COLUMN_UPDATED));
            task.updated = new Date(updated);

            task.due = new Date(c.getLong(c.getColumnIndexOrThrow(COLUMN_DUE)));


            return task;
        }

        private static ContentValues getValues(Task task)
        {
            ContentValues values = new ContentValues();
            values.put(Tasks.COLUMN_TITLE, task.title);
            values.put(Tasks.COLUMN_NOTES, task.notes);
            values.put(Tasks.COLUMN_COMPLETE, task.completed);
            values.put(Tasks.COLUMN_DUE, task.due.getTime());
            values.put(Tasks.COLUMN_DELETED, task.deleted);
            values.put(Tasks.COLUMN_UPDATED, task.updated.getTime());
            return values;
        }

        public void add(Task task)
        {
            Log.d(TAG, "addTask: " + task.title);

            ContentValues values = getValues(task);
            values.put(COLUMN_ID, task.id);
            values.put(COLUMN_LISTID, task.listId);

            parent.insert(TABLE_NAME, values);
        }

        public void delete(Task task)
        {
            Log.d(TAG, "deleteTask: " + task.title);
            String where = COLUMN_ID + "='" + task.id + "' AND " + COLUMN_LISTID + "='" + task.listId + "'";
            parent.delete(TABLE_NAME, where);
        }

        public void update(Task task)
        {
            Log.d(TAG,"updateTask");
            String where = String.format("%s='%s' AND %s='%s'", COLUMN_ID, task.id, COLUMN_LISTID, task.listId);
            ContentValues values = getValues(task);

            parent.update(TABLE_NAME, values, where);
        }

        /**
         * Get all tasks from list
         * @param listId ID of list
         * @return list of tasks
         */
        public List<Task> getList(String listId) {
            return getList(listId,true);
        }

        /**
         * Get all tasks from list
         * @param listId ID of list
         * @param bIncludeBlanks option to exclude blank records which can easily get added on the web
         * @return list of tasks
         */
        public List<Task> getList(String listId, boolean bIncludeBlanks)
        {
            SQLiteDatabase db = parent.openReadOnly();
            String sWhere = null;
            if(listId.length() > 0) // TODO should just be a new function to get ALL tasks
                sWhere = COLUMN_LISTID + "='" + listId + "'";

            //Should be handled outside of database as needed
            //String orderBy = COLUMN_DELETED + " ASC, " + COLUMN_COMPLETE + " ASC, " + COLUMN_TITLE + " ASC";

            Cursor c = db.query(TABLE_NAME, null, sWhere, null, null, null, null);
            ArrayList<Task> result = new ArrayList<>();

            if(c != null)
            {
                while (c.moveToNext())
                {
                    Task task = Tasks.get(c);
                    if(bIncludeBlanks || !task.isBlank())
                        result.add(task);
                }
                c.close();
            }

            return result;
        }

        public void clearCompleted(TaskList list) {
            String sWhere = COLUMN_COMPLETE + "=1 AND " + COLUMN_DELETED + "=0";
            if(!list.isAllTasks())
                sWhere += " AND " + COLUMN_LISTID + "='" + list.id + "'";

            ContentValues values = new ContentValues();
            values.put(COLUMN_DELETED,1);
            values.put(COLUMN_UPDATED,System.currentTimeMillis());

            parent.update(TABLE_NAME, values, sWhere);
        }
    }

    public void setTaskIds(Task task, String sNewId, String sNewListId)
    {
        String where = String.format("%s='%s' AND %s='%s'", Tasks.COLUMN_ID, task.id, Tasks.COLUMN_LISTID, task.listId);
        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_ID, sNewId);
        if(sNewListId != null)
            values.put(Tasks.COLUMN_LISTID, sNewListId);

        update(Tasks.TABLE_NAME, values, where);

    }

    private void logListsTable()
    {
        SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Log.d(TAG,"--- Table: " + TaskLists.TABLE_NAME);
        List<TaskList> lists = this.taskLists.getList();
        for(TaskList list : lists)
        {
            String time = "0";
            if(list.getUpdated().getTime() > 0)
                time = mDateFormat.format(list.getUpdated());

            String id = String.format("%1$-" + 43 + "s", list.id);
            Log.d(TAG,time + "   " + id + " " + list.title);
        }
    }

    private void logTasksTable()
    {
        Log.d(TAG,"--- Table: " + Tasks.TABLE_NAME);
        List<Task> tasks = this.tasks.getList(null);
        for(Task task : tasks)
        {
            String listid = String.format("%1$-" + 43 + "s", task.listId);
            String id =     String.format("%1$-" + 55 + "s", task.id);
            Log.d(TAG,listid + " " + id + " " + task.title);
        }
    }

    public void log()
    {
        logListsTable();
        logTasksTable();
    }
}