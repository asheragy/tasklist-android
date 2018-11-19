package org.cerion.tasklist.data;

import android.content.Context;
import android.util.Log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@androidx.room.Database(entities = {TaskList.class, Task.class}, version = 1, exportSchema = false)
@TypeConverters(DateConverter.class)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "tasks3.db";
    private static volatile  AppDatabase instance;
    private static final Object LOCK = new Object();

    public abstract TaskListDao taskListDao();
    public abstract TaskDao taskDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, DATABASE_NAME).allowMainThreadQueries().build();
                }
            }
        }

        return instance;
    }

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
    public void log() {
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        List<TaskList> lists = taskListDao().getAll();
        Log.d(TAG, "---------------------- App Database ----------------------");

        for (TaskList list : lists) {
            Log.d(TAG, list.logString(dateFormat));
            logTasks(list.getId());
        }

        Log.d(TAG, "----------------------------------------------------------");
    }

    private final String TAG = AppDatabase.class.getSimpleName();
    private void logTasks(String listId) {
        List<Task> tasks = taskDao().getAllbyList(listId);
        for (Task task : tasks) {
            String listid = String.format("%1$-" + 43 + "s", task.listId);
            String id = String.format("%1$-" + 55 + "s", task.id);
            Log.d(TAG, "\t" + listid + " " + id + " " + task.title);
        }
    }

}
