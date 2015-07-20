package org.cerion.todolist;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Database extends SQLiteOpenHelper
{
    public static final String TAG = "Database";
    public static final int DATABASE_VERSION = 5;
    public static final String DATABASE_NAME = "tasks.db";

    //public static final int QUERY_INSERT = 0;
    //public static final int QUERY_UPDATE = 1;
    //public static final int QUERY_DELETE = 2;

    //Singleton class
    private static Database mInstance;
    private Database(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    public synchronized static Database getInstance(Context context)
    {
        if(mInstance == null)
            mInstance = new Database(context.getApplicationContext());

        return mInstance;
    }


    private void insert(String sTable, ContentValues values)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        long result = db.insert(sTable, null, values);
        if(result < 0)
            Log.e(TAG, "insert: " + values.toString());
    }

    private void update(String sTable, ContentValues values, String sWhere)
    {
        SQLiteDatabase db = this.getWritableDatabase();

        long result = db.update(sTable, values, sWhere, null);
        if(result < 0)
            Log.e(TAG, "update: " + values.toString() + " where: " + sWhere);
    }

    private void delete(String sTable, String sWhere)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(sTable, sWhere, null);
        if(result < 0)
            Log.e(TAG, "delete: " + sWhere);
        else if(result > 1)
            Log.d(TAG, "deleted " + result + " rows");
    }

    private static abstract class Sync
    {
        public static final String TABLE_NAME = "sync";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_VALUE = "value";
        public static final String SQL_CREATE = "create table " + TABLE_NAME + "(key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (key))";
    }

    private static abstract class TaskLists
    {
        public static final String TABLE_NAME = "tasklists";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_RENAMED = "renamed";

        public static final String SQL_CREATE = "create table " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_RENAMED   + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id))";

        private static TaskList get(Cursor c)
        {
            String title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE));
            String id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID));
            int renamed = c.getInt(c.getColumnIndexOrThrow(COLUMN_RENAMED));

            return new TaskList(id,title,(renamed == 1 ? true : false));
        }

    }

    public static class Tasks
    {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_LISTID = "listid";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_UPDATED = "updated";
        public static final String COLUMN_NOTES = "notes";
        public static final String COLUMN_COMPLETE = "complete";
        public static final String COLUMN_DUE = "due";
        public static final String COLUMN_DELETED = "deleted";

        public static final String SQL_CREATE = "create table " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_LISTID + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_UPDATED   + " INTEGER, "
                + COLUMN_NOTES   + " TEXT NOT NULL, "
                + COLUMN_DUE   + " INTEGER, "
                + COLUMN_COMPLETE   + " INTEGER DEFAULT 0, "
                + COLUMN_DELETED   + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id,listid))";

        private static Task get(Cursor c)
        {
            String list = c.getString(c.getColumnIndexOrThrow(COLUMN_LISTID));
            String id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID));
            Task task = new Task(list,id);
            task.title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE));
            task.notes = c.getString(c.getColumnIndexOrThrow(COLUMN_NOTES));
            task.deleted = (c.getInt(c.getColumnIndexOrThrow(COLUMN_DELETED)) == 1);

            long updated = c.getLong(c.getColumnIndexOrThrow(COLUMN_UPDATED));
            task.updated = new Date(updated);

            task.due = new Date(c.getLong(c.getColumnIndexOrThrow(COLUMN_DUE)));


            return task;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL(Sync.SQL_CREATE);
        db.execSQL(Tasks.SQL_CREATE);
        db.execSQL(TaskLists.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        Log.d(TAG, "onUpgrade");
        db.execSQL("DROP TABLE IF EXISTS " + Sync.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + Tasks.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TaskLists.TABLE_NAME);
        onCreate(db);
    }

    public void addTask(Task task)
    {
        Log.d(TAG,"addTask: " + task.title);

        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_ID, task.id);
        values.put(Tasks.COLUMN_LISTID, task.listId);
        values.put(Tasks.COLUMN_TITLE, task.title);
        values.put(Tasks.COLUMN_NOTES, task.notes);
        insert(Tasks.TABLE_NAME, values);
    }

    public void deleteTask(Task task)
    {
        Log.d(TAG, "deleteTask: " + task.title);
        String where = Tasks.COLUMN_ID + "='" + task.id + "' AND " + Tasks.COLUMN_LISTID + "='" + task.listId + "'";
        delete(Tasks.TABLE_NAME, where);
    }

    public void updateTask(Task task)
    {
        Log.d(TAG,"updateTask");

        //String where = Tasks.COLUMN_ID + "='" + task.id + "' AND " + Tasks.COLUMN_LISTID + "='" + task.listId + "'";
        String where = String.format("%s='%s' AND %s='%s'", Tasks.COLUMN_ID, task.id, Tasks.COLUMN_LISTID, task.listId);

        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_TITLE, task.title);
        values.put(Tasks.COLUMN_NOTES, task.notes);
        values.put(Tasks.COLUMN_DELETED, task.deleted);
        values.put(Tasks.COLUMN_UPDATED, task.updated.getTime());
        values.put(Tasks.COLUMN_DUE, task.due.getTime());

        update(Tasks.TABLE_NAME, values, where);
    }

    public void addTaskList(TaskList taskList)
    {
        Log.d(TAG,"addTaskList: " + taskList.title);
        ContentValues values = new ContentValues();
        values.put(TaskLists.COLUMN_ID, taskList.id);
        values.put(TaskLists.COLUMN_TITLE, taskList.title);

        insert(TaskLists.TABLE_NAME, values);
    }

    public void updateTaskList(TaskList taskList)
    {
        String where = TaskLists.COLUMN_ID + "='" + taskList.id + "'";
        ContentValues values = new ContentValues();
        values.put(TaskLists.COLUMN_TITLE, taskList.title);
        if(taskList.hasRenamed())
            values.put(TaskLists.COLUMN_RENAMED, (taskList.isRenamed() ? 1 : 0) );

        update(TaskLists.TABLE_NAME, values, where);
    }

    public void setTaskListId(TaskList taskList, String sNewId)
    {
        String where = TaskLists.COLUMN_ID + "='" + taskList.id + "'";
        ContentValues values = new ContentValues();
        values.put(TaskLists.COLUMN_ID, sNewId);

        update(TaskLists.TABLE_NAME, values, where);

        //TODO, update tasks by replacing list id
    }


    public void deleteTaskList(TaskList taskList)
    {
        Log.d(TAG, "deleteTaskList: " + taskList.title);
        String where = TaskLists.COLUMN_ID + "='" + taskList.id + "'";
        delete(TaskLists.TABLE_NAME, where);

        //Also delete tasks linked to this list
        where = Tasks.COLUMN_LISTID + "='" + taskList.id + "'";
        delete(Tasks.TABLE_NAME, where);
    }

    public ArrayList<TaskList> getTaskLists()
    {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor c = db.query(TaskLists.TABLE_NAME,null,null,null,null,null, null);
        ArrayList<TaskList> result = new ArrayList<>();

        if(c != null && c.moveToFirst())
        {
            do
            {
                TaskList list = TaskLists.get(c);
                result.add(list);
            }
            while (c.moveToNext());
        }

        return result;
    }

    public ArrayList<Task> getTasks(String listId)
    {
        SQLiteDatabase db = this.getReadableDatabase();
        String sWhere = null;
        if(listId != null)
            sWhere = Tasks.COLUMN_LISTID + "='" + listId + "'";

        Cursor c = db.query(Tasks.TABLE_NAME,null,sWhere,null,null,null, null);
        ArrayList<Task> result = new ArrayList<>();

        if(c != null && c.moveToFirst())
        {
            do
            {
                Task task = Tasks.get(c);
                result.add(task);
            }
            while (c.moveToNext());
        }

        return result;
    }

    public void setSyncKey(String key, String value)
    {
        SQLiteDatabase db = getWritableDatabase();

        //Workaround for insert or update
        String sWhere = Sync.COLUMN_KEY + "='" + key + "'";
        Cursor c = db.query(Sync.TABLE_NAME,null,sWhere,null,null,null, null);
        boolean bInsert = true;
        if(c != null) //key exists so update instead of insert
        {
            if(c.moveToFirst())
                bInsert = false;
            c.close();
        }

        ContentValues values = new ContentValues();
        values.put(Sync.COLUMN_KEY, key);
        values.put(Sync.COLUMN_VALUE, value);

        if(bInsert)
            db.insert(Sync.TABLE_NAME,null,values);
        else
            db.update(Sync.TABLE_NAME,values,sWhere,null);
    }

    public Map<String,String> getSyncKeys()
    {
        SQLiteDatabase db = getReadableDatabase();

        HashMap<String,String> result = new HashMap<>();
        Cursor c = db.query(Sync.TABLE_NAME, new String[]{Sync.COLUMN_KEY, Sync.COLUMN_VALUE}, null, null, null, null, null);
        if(c != null)
        {
            while(c.moveToNext())
            {
                String key = c.getString(c.getColumnIndexOrThrow(Sync.COLUMN_KEY));
                String value = c.getString(c.getColumnIndexOrThrow(Sync.COLUMN_VALUE));
                result.put(key, value);
            }

            c.close();
        }

        return result;
    }

    public void print()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d(TAG,"--- Database ---");

        /*
        String[] projection = { TaskLists.COLUMN_ID, TaskLists.COLUMN_TITLE };
        Cursor c = db.query(TaskLists.TABLE_NAME,projection,null,null,null,null, null);

        if(c != null && c.moveToFirst())
        {
            do
            {
                String s = c.getString(c.getColumnIndexOrThrow(TaskLists.COLUMN_ID));
                s += "\t" + c.getString(c.getColumnIndexOrThrow(TaskLists.COLUMN_TITLE));
                Log.d(TAG, s);
            }
            while (c.moveToNext());
        }
        */

        //Cursor c = db.query(Sync.TABLE_NAME,new String[]{ Sync.COLUMN_KEY, Sync.COLUMN_VALUE },null,null,null,null, null);
        Cursor c = db.query(Tasks.TABLE_NAME,new String[]{ Tasks.COLUMN_ID, Tasks.COLUMN_TITLE },null,null,null,null, null);

        if(c != null)
        {
            while(c.moveToNext())
            {
                String s = c.getString(0) + "\t" + c.getString(1);
                //String s = c.getString(c.getColumnIndexOrThrow(Sync.COLUMN_KEY)) + "\t" + c.getString(c.getColumnIndexOrThrow(Sync.COLUMN_VALUE))
                Log.d(TAG, s);
            }
            c.close();
        }

        Log.d(TAG,"----- END -----");
    }


}