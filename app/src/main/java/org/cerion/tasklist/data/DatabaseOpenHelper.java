package org.cerion.tasklist.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "tasks.db";
    private static DatabaseOpenHelper mInstance;

    private DatabaseOpenHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public synchronized static DatabaseOpenHelper getInstance(Context context)
    {
        if(mInstance == null)
            mInstance = new DatabaseOpenHelper(context.getApplicationContext());

        return mInstance;
    }

    static class TaskLists {
        public static final String TABLE_NAME = "tasklists";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_RENAMED = "isRenamed";
        public static final String COLUMN_DEFAULT = "isDefault";
        public static final String COLUMN_UPDATED = "updated";

        private static final String SQL_CREATE = "create table " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_RENAMED   + " INTEGER DEFAULT 0, "
                + COLUMN_DEFAULT   + " INTEGER DEFAULT 0, "
                + COLUMN_UPDATED   + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id))";
    }

    static class Tasks {
        public static final String TABLE_NAME = "tasks";
        public static final String COLUMN_ID = "id";
        public static final String COLUMN_LISTID = "listid";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_UPDATED = "updated";
        public static final String COLUMN_NOTES = "notes";
        public static final String COLUMN_COMPLETE = "complete";
        public static final String COLUMN_DUE = "due";
        public static final String COLUMN_DELETED = "deleted";

        private static final String SQL_CREATE = "create table " + TABLE_NAME + "("
                + COLUMN_ID + " TEXT NOT NULL, "
                + COLUMN_LISTID + " TEXT NOT NULL, "
                + COLUMN_TITLE + " TEXT NOT NULL, "
                + COLUMN_UPDATED + " INTEGER DEFAULT 0, "
                + COLUMN_NOTES + " TEXT NOT NULL, "
                + COLUMN_DUE + " INTEGER NOT NULL, "
                + COLUMN_COMPLETE + " INTEGER DEFAULT 0, "
                + COLUMN_DELETED + " INTEGER DEFAULT 0, "
                + "PRIMARY KEY (id,listid), "
                + "FOREIGN KEY (" + COLUMN_LISTID + ") REFERENCES " + TaskLists.TABLE_NAME + "(" + TaskLists.COLUMN_ID + ") ON UPDATE CASCADE"
                + ")";
    }

        @Override
    public void onCreate(SQLiteDatabase db)
    {
        db.execSQL("PRAGMA foreign_keys=ON;");
        db.execSQL(TaskLists.SQL_CREATE);
        db.execSQL(Tasks.SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP TABLE IF EXISTS " + Tasks.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TaskLists.TABLE_NAME);
        onCreate(db);
    }
}
