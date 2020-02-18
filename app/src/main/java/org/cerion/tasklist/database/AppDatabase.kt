package org.cerion.tasklist.database

import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import org.cerion.tasklist.common.TAG
import java.text.SimpleDateFormat
import java.util.*

// TODO if resetting database version
//   - rename TaskList.lastSync field so @column is not needed

@androidx.room.Database(entities = [TaskList::class, Task::class], version = 4, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun taskListDao(): TaskListDao
    abstract fun taskDao(): TaskDao
    fun log() {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val lists = taskListDao().getAll()
        Log.i(TAG, "---------------------- App Database ----------------------")

        for (list in lists) {
            Log.i(TAG, list.logString(dateFormat))
            logTasks(list.id)
        }

        Log.i(TAG, "----------------------------------------------------------")
    }

    private fun logTasks(listId: String) {
        val tasks = taskDao().getAllByList(listId)
        for (task in tasks) {
            Log.i(TAG, "    " + task.logString(dateFormat))
        }
    }

    private fun verifyDefaultTaskList() {
        // TODO verify this works on clean install
        if(taskListDao().getAll().isEmpty()) {
            val defaultList = TaskList(generateTempId(), "Default")
            defaultList.isDefault = true
            taskListDao().add(defaultList)
        }
    }

    companion object {

        private const val DATABASE_NAME = "tasks.db"
        @Volatile
        private var instance: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE " + TaskList.TABLE_NAME + " ADD COLUMN deleted INTEGER DEFAULT 0 NOT NULL")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE " + TaskList.TABLE_NAME + " ADD COLUMN updated_tasks INTEGER DEFAULT 0 NOT NULL")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `tasklists_new` (`updated` INTEGER NOT NULL, `updated_tasks` INTEGER NOT NULL, `isDefault` INTEGER NOT NULL, `deleted` INTEGER NOT NULL, `id` TEXT NOT NULL, `title` TEXT NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("INSERT INTO tasklists_new (updated, updated_tasks, isDefault, deleted, id, title) SELECT updated, updated_tasks, isDefault, deleted, id, title FROM tasklists")
                database.execSQL("DROP TABLE tasklists")
                database.execSQL("ALTER TABLE tasklists_new RENAME TO tasklists")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            synchronized(this) {
                var db = instance
                if (db == null) {
                    db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, DATABASE_NAME)
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                            .build()

                    db.verifyDefaultTaskList()
                    instance = db
                }

                return db
            }

        }

        fun generateTempId(): String {
            val rand = Random()
            val i = rand.nextInt() + (1L shl 31)
            return "temp_$i"
        }

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    }

}
