package org.cerion.tasklist

import android.util.Log
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.cerion.tasklist.data.AppDatabase
import org.cerion.tasklist.data.TaskList
import org.cerion.tasklist.data.TaskListDao
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

    @Before
    fun createDb() {
        Log.i("TEST", "START")
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        listDao = db.taskListDao()

        listDao.add( TaskList("123", "Default List", Date(), false, true))
        listDao.add( TaskList("456", "Test List", Date(), false, false))
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun init() {
        val lists = listDao.all
        assertEquals(2, lists.size)

        assertEquals("123", lists.filter { it.isDefault }.firstOrNull()!!.id)
        assertEquals("Test List", lists.filter { it.id == "456" }.firstOrNull()!!.title)
    }

    @Test
    fun list_update() {
        var list = listDao.all.first { it.id == "123" }
        list.title = "new title"
        list.updated = Date(1)
        list.isRenamed = true
        list.isDefault = false

        listDao.update(list)

        list = listDao.all.first { it.id == "123" }
        assertEquals("new title", list.title)
        assertEquals(1, list.updated.time)
        assertEquals(true, list.isRenamed)
        assertEquals(false, list.isDefault)
    }

    @Test
    fun list_setId() {
        listDao.setId("123", "999")

        assertTrue( listDao.all.any { it.id == "999" })
    }

    @Test
    fun list_setLastUpdated() {
        listDao.setLastUpdated("123", Date(1234567890))

        val list = listDao.all[0]
        assertEquals(1234567890, list.updated.time)
    }
}
