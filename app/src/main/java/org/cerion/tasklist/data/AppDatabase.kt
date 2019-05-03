package org.cerion.tasklist.data

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.text.SimpleDateFormat
import java.util.*

@androidx.room.Database(entities = [TaskList::class, Task::class], version = 2, exportSchema = false)
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
            Log.d(TAG, "    " + task.logString(dateFormat))
        }
    }

    companion object {
        val TAG: String = AppDatabase::class.java.simpleName

        private const val DATABASE_NAME = "tasks.db"
        @Volatile
        private var instance: AppDatabase? = null
        private val LOCK = Any()

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE " + TaskList.TABLE_NAME + " ADD COLUMN deleted INTEGER DEFAULT 0 NOT NULL")
            }
        }

        fun getInstance(context: Context): AppDatabase? {
            if (instance == null) {
                synchronized(LOCK) {
                    if (instance == null) {
                        instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                                .allowMainThreadQueries()
                                .addMigrations(MIGRATION_1_2)
                                .build()
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
