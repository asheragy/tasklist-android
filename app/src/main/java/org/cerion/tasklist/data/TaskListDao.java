package org.cerion.tasklist.data;

import java.util.List;

import androidx.room.Dao;
import androidx.room.Query;

@Dao
public interface TaskListDao {

    @Query("SELECT * FROM " + TaskList.TABLE_NAME)
    List<TaskList> getAll();

    /*

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
            if(taskList.hasRenamed())
                values.put(COLUMN_RENAMED, (taskList.isRenamed() ? 1 : 0) );

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
     */
}
