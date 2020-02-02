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


internal class SyncTest {

    private val listDao: TaskListDao = mock()
    private val taskDao: TaskDao = mock()
    private val remoteRepo: GoogleTasksRepository = mock()
    private val prefs: Prefs = mock()

    private val sync = Sync(listDao, taskDao, remoteRepo, prefs)
    private val baseModifiedTime = Date(1577836800000) // UTC Midnight 1/1/2020
    private val defaultList = TaskList("1","Default").apply { isDefault = true; updated = baseModifiedTime }

    @BeforeEach
    fun beforeEachTest() {
        whenever(listDao.getAll()).thenReturn(listOf(defaultList))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(Task("1").apply { id = "A"; title = "task A"; updated = baseModifiedTime }))
        whenever(remoteRepo.getLists()).thenReturn(listOf(TaskList("1","Default").apply { isDefault = true; updated = baseModifiedTime }))

        // Default successful response
        whenever(remoteRepo.createList(any())).thenAnswer { it.arguments[0] }
        whenever(remoteRepo.updateList(any())).thenReturn(true)
        whenever(remoteRepo.deleteList(any())).thenReturn(true)
        whenever(remoteRepo.createTask(any())).thenAnswer { it.arguments[0]}
        whenever(remoteRepo.updateTask(any())).thenReturn(true)
        whenever(remoteRepo.deleteTask(any())).thenReturn(true)
    }

    @Test
    fun `happy path has no changes`() {
        val result = sync.run()

        assertTrue(result.success)
        assertEquals(0, result.totalChanges)
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

    @Test
    fun `lists local to remote add+change`() {
        whenever(listDao.getAll()).thenReturn(listOf(
                defaultList.fullCopy().apply { title = "Default Modified"; isRenamed = true },
                TaskList("temp_id", "Another list")
        ))

        val result = sync.run()
        assertEquals(2, result.totalChanges)
        assertEquals(1, result.listsToRemote.add)
        assertEquals(1, result.listsToRemote.change)
        // Deleted is a special case depending on if tasks exist

        result.listsToLocal.change++ // offset call to local db to clear isRenamed
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `lists local to remote delete`() {
        whenever(listDao.getAll()).thenReturn(listOf(defaultList, TaskList("2", "List B").apply { deleted = true }))
        whenever(remoteRepo.getLists()).thenReturn(listOf(defaultList, TaskList("2", "List B")))
        whenever(taskDao.getAllByList(eq("2"))).thenReturn(listOf(Task("2")))

        // Nothing if list is non-empty
        var result = sync.run()
        assertEquals(0, result.totalChanges)

        // Deletes if empty
        whenever(taskDao.getAllByList(eq("2"))).thenReturn(emptyList())
        result = sync.run()
        assertEquals(1, result.totalChanges)
        assertEquals(1, result.listsToRemote.delete)
    }

    @Test
    fun `lists remote to local`() {
        whenever(listDao.getAll()).thenReturn(listOf(
                defaultList,
                TaskList("3", "List not on remote")
        ))
        whenever(remoteRepo.getLists()).thenReturn(listOf(
                defaultList.fullCopy().apply { title = "New Title" },
                TaskList("2", "List Added")
        ))

        val result = sync.run()

        assertEquals(3, result.totalChanges)
        assertEquals(1, result.listsToLocal.add)
        assertEquals(1, result.listsToLocal.change)
        assertEquals(1, result.listsToLocal.delete)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `tasks local to remote`() {
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(
                Task("1").apply { updated = baseModifiedTime },
                Task("1").apply { id = "abc"; updated = Date(baseModifiedTime.time + 1) },
                Task("1").apply { id = "def"; deleted = true }
        ))

        val result = sync.run()
        assertEquals(3, result.totalChanges)
        assertEquals(1, result.tasksToRemote.add)
        assertEquals(1, result.tasksToRemote.change)
        assertEquals(1, result.tasksToRemote.delete)

        // remote add triggers local delete+add to update ID, may change this later...
        result.tasksToLocal.delete++
        result.tasksToLocal.add++
        // remote delete removes local record as well
        result.tasksToLocal.delete++
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `tasks remote to local`() {
        whenever(remoteRepo.getLists()).thenReturn(listOf(TaskList("1","Default").apply { isDefault = true; updated = Date(baseModifiedTime.time + 1) }))
        whenever(remoteRepo.getTasks(eq("1"), any())).thenReturn(listOf(
                Task("1"),
                Task("1").apply { id = "abc" },
                Task("1").apply { id = "def"; deleted = true }
        ))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(
                Task("1").apply { id = "abc" },
                Task("1").apply { id = "def" }
        ))

        val result = sync.run()
        assertEquals(3, result.totalChanges)
        assertEquals(1, result.tasksToLocal.add)
        assertEquals(1, result.tasksToLocal.change)
        assertEquals(1, result.tasksToLocal.delete)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `tasks remote to local ignore unsynced deletions`() {
        whenever(remoteRepo.getLists()).thenReturn(listOf(TaskList("1","Default").apply { isDefault = true; updated = Date(baseModifiedTime.time + 1) }))
        whenever(remoteRepo.getTasks(eq("1"), any())).thenReturn(listOf(Task("1").apply { id = "def"; deleted = true }))
        whenever(taskDao.getAllByList(eq("1"))).thenReturn(listOf(Task("1").apply { id = "abc" }))

        val result = sync.run()
        assertEquals(0, result.totalChanges)
        verifyCallsMatchChanges(result)
    }

    private fun verifyCallsMatchChanges(result: SyncResult) {
        verify(remoteRepo, times(result.listsToRemote.add)).createList(any())
        verify(remoteRepo, times(result.listsToRemote.change)).updateList(any())
        verify(remoteRepo, times(result.listsToRemote.delete)).deleteList(any())

        verify(remoteRepo, times(result.tasksToRemote.add)).createTask(any())
        verify(remoteRepo, times(result.tasksToRemote.change)).updateTask(any())
        verify(remoteRepo, times(result.tasksToRemote.delete)).deleteTask(any())

        verify(listDao, times(result.listsToLocal.add)).add(any())
        verify(listDao, times(result.listsToLocal.change)).update(any())
        verify(listDao, times(result.listsToLocal.delete)).delete(any())

        verify(taskDao, times(result.tasksToLocal.add)).add(any())
        verify(taskDao, times(result.tasksToLocal.change)).update(any())
        verify(taskDao, times(result.tasksToLocal.delete)).delete(any())
    }

}

fun TaskList.fullCopy() = TaskList(id, title).also { copy ->
    copy.deleted = deleted
    copy.isRenamed = isRenamed
    copy.updated = updated
    copy.isDefault = isDefault
}