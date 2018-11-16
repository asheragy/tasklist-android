package org.cerion.tasklist.data;

import java.util.Date;
import java.util.List;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface TaskListDao {

    @Query("SELECT * FROM " + TaskList.TABLE_NAME)
    List<TaskList> getAll();

    @Insert
    void add(TaskList taskList);

    @Update
    void update(TaskList taskList);

    // TODO test updating the list id cascades to the tasks
    @Query("UPDATE " + TaskList.TABLE_NAME  + " SET id = :newId WHERE id = :oldId")
    void setId(String oldId, String newId);

    //This is only set after tasks have successfully synced, so it needs to be updated on its own
    @Query("UPDATE " + TaskList.TABLE_NAME  + " SET updated = :updated WHERE id = :id")
    void setLastUpdated(String id, Date updated);

    /*
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
     */
}
