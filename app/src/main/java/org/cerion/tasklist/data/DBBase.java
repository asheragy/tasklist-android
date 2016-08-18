package org.cerion.tasklist.data;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

abstract class DBBase {

    private final SQLiteOpenHelper mOpenHelper;
    private static final String TAG = DBBase.class.getSimpleName();

    DBBase(SQLiteOpenHelper openHelper) {
        mOpenHelper = openHelper;
    }

    SQLiteDatabase open()
    {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON;");
        return db;
    }

    SQLiteDatabase openReadOnly()
    {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        db.execSQL("PRAGMA foreign_keys = ON;");
        return db;
    }

    void insert(String sTable, ContentValues values)
    {
        SQLiteDatabase db = open();
        insert(db,sTable,values);
        db.close();
    }

    void insert(SQLiteDatabase db, String sTable, ContentValues values)
    {
        long result = db.insert(sTable, null, values);
        if(result < 0)
            Log.e(TAG, "insert: " + values.toString());
    }

    void update(String sTable, ContentValues values, String sWhere)
    {
        SQLiteDatabase db = open();
        update(db,sTable,values,sWhere);
        db.close();
    }

    void update(SQLiteDatabase db, String sTable, ContentValues values, String sWhere)
    {
        long result = db.update(sTable, values, sWhere, null);
        if(result < 0)
            Log.e(TAG, "update: " + values.toString() + " where: " + sWhere);
        else if(result >= 0)
            Log.d(TAG, "updated " + result + " rows");
    }

    void delete(String sTable, String sWhere)
    {
        SQLiteDatabase db = open();
        delete(db,sTable,sWhere);
        db.close();
    }

    void delete(SQLiteDatabase db, String sTable, String sWhere)
    {
        long result = db.delete(sTable, sWhere, null);
        if(result < 0)
            Log.e(TAG, "delete: " + sWhere);
        else
            Log.d(TAG, "deleted " + result + " rows");
    }
}
