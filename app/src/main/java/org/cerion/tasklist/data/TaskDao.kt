package org.cerion.tasklist.data

import androidx.room.*

@Dao
interface TaskDao {

    @Query("SELECT * FROM " + Task.TABLE_NAME)
    fun getAll(): List<Task>

    @Query("SELECT * FROM " + Task.TABLE_NAME + " WHERE listId = :listId")
    fun getAllbyList(listId: String): List<Task>

    @Insert
    fun add(task: Task)

    @Update
    fun update(task: Task)

    @Delete
    fun delete(task: Task)

    /*
        public void setTaskIds(Task task, String sNewId, String sNewListId)
    {
        String where = String.format("%s='%s' AND %s='%s'", Tasks.COLUMN_ID, task.id, Tasks.COLUMN_LISTID, task.listId);
        ContentValues values = new ContentValues();
        values.put(Tasks.COLUMN_ID, sNewId);
        if(sNewListId != null)
            values.put(Tasks.COLUMN_LISTID, sNewListId);

        update(Tasks.TABLE_NAME, values, where);

    }
     */
    //@Query("UPDATE " + Task.TABLE_NAME + " SET deleted=1 WHERE listId=:listId AND id=:id")
    //void delete(String listId, String id);

    //@Query("UPDATE " + Task.TABLE_NAME + " SET deleted=1, updated=now() WHERE completed=1 AND deleted=0 AND listId = :listId")
    //void clearCompleted(String listId);

    /*
    CURRENT_TIMESTAMP
    public void clearCompleted(TaskList list) {
        String sWhere = COLUMN_COMPLETE + "=1 AND " + COLUMN_DELETED + "=0";
        if(!list.isAllTasks())
            sWhere += " AND " + COLUMN_LISTID + "='" + list.id + "'";

        ContentValues values = new ContentValues();
        values.put(COLUMN_DELETED,1);
        values.put(COLUMN_UPDATED,System.currentTimeMillis());

        parent.update(TABLE_NAME, values, sWhere);
    }
     */

    /*
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
    */

}
