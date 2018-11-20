package org.cerion.tasklist.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import java.text.SimpleDateFormat
import java.util.*

@androidx.room.Database(entities = [TaskList::class, Task::class], version = 1, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskListDao(): TaskListDao
    abstract fun taskDao(): TaskDao
    fun log() {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val lists = taskListDao().getAll()
        Log.d(TAG, "---------------------- App Database ----------------------")

        for (list in lists) {
            Log.d(TAG, list.logString(dateFormat))
            logTasks(list.id)
        }

        Log.d(TAG, "----------------------------------------------------------")
    }

    private fun logTasks(listId: String) {
        val tasks = taskDao().getAllbyList(listId)
        for (task in tasks) {
            val listid = String.format("%1$-" + 43 + "s", task.listId)
            val id = String.format("%1$-" + 55 + "s", task.id)
            Log.d(TAG, "\t" + listid + " " + id + " " + task.title)
        }
    }

    companion object {
        val TAG: String = AppDatabase::class.java.simpleName

        private const val DATABASE_NAME = "tasks.db"
        @Volatile
        private var instance: AppDatabase? = null
        private val LOCK = Any()

        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                synchronized(LOCK) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME).allowMainThreadQueries().build()
                    }
                }
            }

            return instance
        }

        fun generateTempId(): String {
            val rand = Random()
            val i = rand.nextInt() + (1L shl 31)
            return "temp_$i"
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

}
