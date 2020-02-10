package org.cerion.tasklist.sync


import com.nhaarman.mockitokotlin2.*
import org.cerion.tasklist.database.*
import org.cerion.tasklist.googleapi.GoogleTasksRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*


internal class SyncTest_Tasks {

    private val listDao: TaskListDao = mock()
    private val taskDao: TaskDao = mock()
    private val remoteRepo: GoogleTasksRepository = mock()
    private val prefs: Prefs = mock()

    private val sync = Sync(listDao, taskDao, remoteRepo, prefs)
    private val baseModifiedTime = Date(1577836800000) // UTC Midnight 1/1/2020
    private val defaultList = TaskList("1","Default").apply { isDefault = true; updated = baseModifiedTime; lastSync = baseModifiedTime }

    @BeforeEach
    fun beforeEachTest() {
        whenever(listDao.getAll()).thenReturn(listOf(defaultList))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(Task("1").apply { id = "A"; title = "task A"; updated = baseModifiedTime }))

        // Default successful response
        whenever(remoteRepo.createTask(any())).thenAnswer { it.arguments[0]}
        whenever(remoteRepo.updateTask(any())).thenAnswer { it.arguments[0]}
        whenever(remoteRepo.deleteTask(any())).thenReturn(true)
    }

    @Test
    fun `happy path has no changes`() {
        val result = sync.tasks("1")

        assertTrue(result.success)
        assertEquals(0, result.totalChanges)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `minimal data calls when no changes`() {
        sync.tasks("1")

        // Get List + update last sync time
        assertEquals(2, Mockito.mockingDetails(listDao).invocations.size)
        // Get tasks for list
        assertEquals(1, Mockito.mockingDetails(taskDao).invocations.size)
        // Get remote tasks
        assertEquals(1, Mockito.mockingDetails(remoteRepo).invocations.size)
        // Nothing
        assertEquals(0, Mockito.mockingDetails(prefs).invocations.size)
    }

    @Test
    fun `tasks local to remote`() {
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(
                Task("1").apply { updated = baseModifiedTime },
                Task("1").apply { id = "abc"; updated = baseModifiedTime.add(1) },
                Task("1").apply { id = "def"; deleted = true }
        ))

        val result = sync.tasks("1")
        assertEquals(3, result.totalChanges)
        assertEquals(1, result.toRemote.add)
        assertEquals(1, result.toRemote.change)
        assertEquals(1, result.toRemote.delete)

        // remote delete removes local record as well
        result.toLocal.delete++
        // remote change updates local with timestamp
        result.toLocal.change++
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `tasks remote to local`() {
        whenever(remoteRepo.getTasks(eq("1"), any())).thenReturn(listOf(
                Task("1").apply { title = "Added" },
                Task("1").apply { title = "Updated"; id = "abc"; updated = baseModifiedTime.add(1) },
                Task("1").apply { title = "Deleted"; id = "def"; deleted = true }
        ))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(
                Task("1").apply { id = "abc" },
                Task("1").apply { id = "def" }
        ))

        val result = sync.tasks("1")
        assertEquals(3, result.totalChanges)
        assertEquals(1, result.toLocal.add)
        assertEquals(1, result.toLocal.change)
        assertEquals(1, result.toLocal.delete)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `tasks remote to local ignore unsynced deletions`() {
        whenever(remoteRepo.getTasks(eq("1"), any())).thenReturn(listOf(Task("1").apply { id = "def"; deleted = true }))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(Task("1").apply { id = "abc" }))

        val result = sync.tasks("1")
        assertEquals(0, result.totalChanges)
        verifyCallsMatchChanges(result)
    }

    private fun verifyCallsMatchChanges(result: SyncResult) {
        verify(remoteRepo, times(result.toRemote.add)).createTask(any())
        verify(remoteRepo, times(result.toRemote.change)).updateTask(any())
        verify(remoteRepo, times(result.toRemote.delete)).deleteTask(any())

        verify(taskDao, times(result.toLocal.add)).add(any())
        verify(taskDao, times(result.toLocal.change)).update(any())
        verify(taskDao, times(result.toLocal.delete)).delete(any())
    }

}
