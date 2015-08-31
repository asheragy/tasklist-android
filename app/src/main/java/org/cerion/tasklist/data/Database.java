package org.cerion.tasklist.data;

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
    private static final String TAG = "Database";
    private static final int DATABASE_VERSION = 6;
    private static final String DATABASE_NAME = "tasks.db";

    public final TaskLists taskLists;
    public final Tasks tasks;

    //Singleton class
    private static Database mInstance;
    private Database(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        taskLists = new TaskLists(this);
        tasks = new Tasks(this);
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
        else if(result >= 0)
            Log.d(TAG, "updated " + result + " rows");
    }

    private void delete(String sTable, String sWhere)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        long result = db.delete(sTable, sWhere, null);
        if(result < 0)
            Log.e(TAG, "delete: " + sWhere);
        else
            Log.d(TAG, "deleted " + result + " rows");
    }

    private static abstract class Sync
    {
        public static final String TABLE_NAME = "sync";
        public static final String COLUMN_KEY = "key";
        public static final String COLUMN_VALUE = "value";
        public static final String SQL_CREATE = "create table " + TABLE_NAME + "(key TEXT NOT NULL, value TEXT NOT NULL, PRIMARY KEY (key))";
    }

    public static class TaskLists
    {
        private final Database parent;
        public static final String TABLE_NAME = "tasklists";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_RENAMED = "isRenamed";
        public static final String COLUMN_DEFAULT = "isDefault";

        TaskLists(Database db)
        {
            parent = db;
        }

        public static final String SQL_CREATE = "create table " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_RENAMED   + " INTEGER DEFAULT 0, "
                + COLUMN_DEFAULT   + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id))";

        private static TaskList get(Cursor c)
        {
            String title = c.getString(c.getColumnIndexOrThrow(COLUMN_TITLE));
            String id = c.getString(c.getColumnIndexOrThrow(COLUMN_ID));
            int renamed = c.getInt(c.getColumnIndexOrThrow(COLUMN_RENAMED));
            int def = c.getInt(c.getColumnIndexOrThrow(COLUMN_DEFAULT));

            TaskList result = new TaskList(id,title, (renamed == 1) );
            result.bDefault = (def == 1);
            return result;
        }

        public ArrayList<TaskList> getList()
        {
            SQLiteDatabase db = parent.getReadableDatabase();

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

            return result;
        }

        public void add(TaskList taskList)
        {
            Log.d(TAG,"addTaskList: " + taskList.title);
            ContentValues values = new ContentValues();
            values.put(COLUMN_ID, taskList.id);
            values.put(COLUMN_TITLE, taskList.title);
            values.put(COLUMN_DEFAULT, taskList.bDefault);

            parent.insert(TABLE_NAME, values);
        }

        public void update(TaskList taskList)
        {
            String where = COLUMN_ID + "='" + taskList.id + "'";
            ContentValues values = new ContentValues();
            values.put(COLUMN_TITLE, taskList.title);
            values.put(COLUMN_DEFAULT, taskList.bDefault);
            if(taskList.hasRenamed())
                values.put(COLUMN_RENAMED, (taskList.isRenamed() ? 1 : 0) );

            parent.update(TABLE_NAME, values, where);
        }

        public void delete(TaskList taskList)
        {
            Log.d(TAG, "deleteTaskList: " + taskList.title);
            String where = COLUMN_ID + "='" + taskList.id + "'";
            parent.delete(TABLE_NAME, where);

            //Also delete tasks linked to this list
            where = Tasks.COLUMN_LISTID + "='" + taskList.id + "'";
            parent.delete(Tasks.TABLE_NAME, where);
        }

    }

    public static class Tasks
    {
        private final Database parent;
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
                + COLUMN_UPDATED   + " INTEGER DEFAULT 0, "
                + COLUMN_NOTES   + " TEXT NOT NULL, "
                + COLUMN_DUE   + " INTEGER NOT NULL, "
                + COLUMN_COMPLETE   + " INTEGER DEFAULT 0, "
                + COLUMN_DELETED   + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id,listid))";

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

        public ArrayList<Task> getList(String listId)
        {
            SQLiteDatabase db = parent.getReadableDatabase();
            String sWhere = null;
            if(listId != null)
                sWhere = COLUMN_LISTID + "='" + listId + "'";

            Cursor c = db.query(TABLE_NAME, null, sWhere, null, null, null, null);
            ArrayList<Task> result = new ArrayList<>();

            if(c != null)
            {
                while (c.moveToNext())
                {
                    Task task = Tasks.get(c);
                    result.add(task);
                }
                c.close();
            }

            return result;
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

    public void setTaskIds(Task task, String sNewId, String sNewListId)
    {
        String where = String.format("%s='%s' AND %s='%s'", Tasks.COLUMN_ID, task.id, Tasks.COLUMN_LISTID, task.listId);
        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_ID, sNewId);
        if(sNewListId != null)
            values.put(Tasks.COLUMN_LISTID, sNewListId);

        update(Tasks.TABLE_NAME, values, where);

    }

    public void setTaskListId(TaskList taskList, String sNewId)
    {
        String where = TaskLists.COLUMN_ID + "='" + taskList.id + "'";
        ContentValues values = new ContentValues();
        values.put(TaskLists.COLUMN_ID, sNewId);
        update(TaskLists.TABLE_NAME, values, where);

        //Update tasks with this list Id
        where = String.format("%s='%s'", Tasks.COLUMN_LISTID, taskList.id);
        values = new ContentValues();
        values.put(Tasks.COLUMN_LISTID, sNewId);
        update(Tasks.TABLE_NAME,values,where);
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

    public void clearSyncKeys()
    {
        delete(Sync.TABLE_NAME,null);
    }

    private void logListsTable()
    {
        Log.d(TAG,"--- Table: " + TaskLists.TABLE_NAME);
        ArrayList<TaskList> lists = this.taskLists.getList();
        for(TaskList list : lists)
        {
            String id = String.format("%1$-" + 43 + "s", list.id);
            Log.d(TAG,id + " " + list.title);
        }
    }

    private void logTasksTable()
    {
        Log.d(TAG,"--- Table: " + Tasks.TABLE_NAME);
        ArrayList<Task> tasks = this.tasks.getList(null);
        for(Task task : tasks)
        {
            String listid = String.format("%1$-" + 43 + "s", task.listId);
            String id =     String.format("%1$-" + 55 + "s", task.id);
            Log.d(TAG,listid + " " + id + " " + task.title);
        }
    }

    private void logSyncTable()
    {
        SQLiteDatabase db = this.getReadableDatabase();
        Log.d(TAG,"--- Table: " + Sync.TABLE_NAME);
        Cursor c = db.query(Sync.TABLE_NAME,new String[]{ Sync.COLUMN_KEY, Sync.COLUMN_VALUE },null,null,null,null, null);

        if(c != null)
        {
            while(c.moveToNext())
            {
                String s0 = c.getString(0);
                String s1 = c.getString(1);
                String s = String.format("%1$-" + 55 + "s", s0) + "\t" + s1;
                Log.d(TAG, s);
            }
            c.close();
        }
    }

    public void log()
    {
        logListsTable();
        logTasksTable();
        logSyncTable();
    }


}