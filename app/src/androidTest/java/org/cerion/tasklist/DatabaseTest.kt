package org.cerion.tasklist

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cerion.tasklist.data.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class DatabaseTest {

    private lateinit var db: AppDatabase
    private lateinit var listDao: TaskListDao
    private lateinit var taskDao: TaskDao

    @Before
    fun createDb() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        listDao = db.taskListDao()
        taskDao = db.taskDao()

        listDao.add( TaskList("123", "Default List", Date(), false, true, false))
        listDao.add( TaskList("456", "Test List", Date(), false, false, false))

        var task = Task("123", "ABC")
        task.title = "Task 1"
        taskDao.add(task)

        task = Task("456", "DEF")
        task.title = "Task 2"
        taskDao.add(task)
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun init_lists() {
        val lists = listDao.getAll()
        assertEquals(2, lists.size)

        assertEquals("123", lists.firstOrNull { it.isDefault }!!.id)
        assertEquals("Test List", lists.firstOrNull { it.id == "456" }!!.title)
    }

    @Test
    fun init_tasks() {
        val tasks = taskDao.getAll()
        assertEquals(2, tasks.size)
    }

    @Test
    fun list_update() {
        var list = listDao.getAll().first { it.id == "123" }
        list.title = "new title"
        list.updated = Date(1)
        list.isRenamed = true
        list.isDefault = false

        listDao.update(list)

        list = listDao.getAll().first { it.id == "123" }
        assertEquals("new title", list.title)
        assertEquals(1, list.updated.time)
        assertEquals(true, list.isRenamed)
        assertEquals(false, list.isDefault)
    }

    @Test
    fun list_setId() {
        listDao.updateId("123", "999")

        assertTrue( listDao.getAll().any { it.id == "999" })
    }

    @Test
    fun list_setLastUpdated() {
        listDao.setLastUpdated("123", Date(1234567890))

        val list = listDao.getAll()[0]
        assertEquals(1234567890, list.updated.time)
    }

    /*
    @Test
    fun list_cascadeDelete() {
        listDao.delete("456")

        assertEquals(1, listDao.getAll().size)
        val tasks = taskDao.all
        assertEquals(1, tasks.size)
        assertFalse("task list not deleted", tasks.any { it.listId == "456" })
    }
    */

    @Test
    fun list_cascaseUpdate() {
        listDao.updateId("123", "999")

        assertTrue("list id did not update", listDao.getAll().any { it.id == "999" })
        assertTrue("task parent list id did not update", taskDao.getAll().any { it.listId == "999" })
    }

    /*
    @Test
    fun task_clearCompleted() {
        var task = Task("123", "AAA")
        task.title = "Completed task"
        task.completed = true
        taskDao.add(task)

        taskDao.clearCompleted("123")

        // Only one task in entire database is deleted
        assertEquals(1, taskDao.all.filter { it.deleted }.size)

        // task is deleted and new updated time
        task = taskDao.getAllbyList("123").first { it.id == "AAA" }
        assertTrue(task.deleted)
        assertTrue(task.updated.time > Date().time - 1000)
    }
    */

}
