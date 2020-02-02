package org.cerion.tasklist.sync


import com.nhaarman.mockitokotlin2.*
import org.cerion.tasklist.database.Prefs
import org.cerion.tasklist.database.TaskDao
import org.cerion.tasklist.database.TaskList
import org.cerion.tasklist.database.TaskListDao
import org.cerion.tasklist.googleapi.GoogleTasksRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*


internal class SyncTest {

    private val listDao: TaskListDao = mock()
    private val taskDao: TaskDao = mock()
    private val remoteRepo: GoogleTasksRepository = mock()
    private val prefs: Prefs = mock()

    private val sync = Sync(listDao, taskDao, remoteRepo, prefs)
    private val baseModifiedTime = Date(1577836800000) // UTC Midnight 1/1/2020

    @BeforeEach
    fun beforeEachTest() {
        whenever(listDao.getAll()).thenReturn(listOf(TaskList("1","Default").apply { isDefault = true; updated = baseModifiedTime }))
        whenever(remoteRepo.getLists()).thenReturn(listOf(TaskList("1","Default").apply { isDefault = true; updated = baseModifiedTime }))
    }

    @Test
    fun `happy path has no changes`() {
        val result = sync.run()

        assertTrue(result.success)
        assertEquals(0, result.toLocal.totalChanges)
        assertEquals(0, result.toRemote.totalChanges)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `minimal data calls when no changes`() {
        sync.run()

        // Get local data to check for changes
        assertEquals(1, Mockito.mockingDetails(listDao).invocations.size)
        assertEquals(1, Mockito.mockingDetails(taskDao).invocations.size)
        // Remote data for changes
        assertEquals(1, Mockito.mockingDetails(remoteRepo).invocations.size)
        // Save last sync time
        assertEquals(1, Mockito.mockingDetails(prefs).invocations.size)
    }

    private fun verifyCallsMatchChanges(result: SyncResult) {
        verify(remoteRepo, times(result.toRemote.listAdd)).createList(any())
        verify(remoteRepo, times(result.toRemote.listChange)).updateList(any())
        verify(remoteRepo, times(result.toRemote.listDelete)).deleteList(any())
        verify(remoteRepo, times(result.toRemote.taskAdd)).createTask(any())
        verify(remoteRepo, times(result.toRemote.taskChange)).updateTask(any())
        verify(remoteRepo, times(result.toRemote.taskDelete)).deleteTask(any())

        verify(listDao, times(result.toLocal.listAdd)).add(any())
        verify(listDao, times(result.toLocal.listChange)).update(any())
        verify(listDao, times(result.toLocal.listDelete)).delete(any())

        verify(taskDao, times(result.toLocal.taskAdd)).add(any())
        verify(taskDao, times(result.toLocal.taskChange)).update(any())
        verify(taskDao, times(result.toLocal.taskDelete)).delete(any())
    }

}