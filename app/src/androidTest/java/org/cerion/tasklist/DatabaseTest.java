package org.cerion.tasklist;

import android.content.Context;
import android.util.Log;

import org.cerion.tasklist.data.AppDatabase;
import org.cerion.tasklist.data.TaskList;
import org.cerion.tasklist.data.TaskListDao;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import androidx.room.Room;
import androidx.test.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class DatabaseTest {

    private AppDatabase db;
    private TaskListDao dao;

    @Before
    public void createDb() {
        Log.i("TEST", "START");
        Context context = InstrumentationRegistry.getTargetContext();
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        dao = db.taskListDao();
    }

    @After
    public void closeDb() {
        db.close();
    }

    @Test
    public void test() {
        List<TaskList> list = dao.getAll();

        assertEquals(0, list.size());
    }

    @Test
    public void test2() {
        assertEquals(0,1);
    }
}
