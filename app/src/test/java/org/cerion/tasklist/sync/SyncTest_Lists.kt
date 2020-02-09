package org.cerion.tasklist.sync


import com.nhaarman.mockitokotlin2.*
import org.cerion.tasklist.database.*
import org.cerion.tasklist.googleapi.GoogleTaskList
import org.cerion.tasklist.googleapi.GoogleTasksRepository
import org.cerion.tasklist.googleapi.toGoogleList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import java.util.*


internal class SyncTest_Lists {

    private val listDao: TaskListDao = mock()
    private val taskDao: TaskDao = mock()
    private val remoteRepo: GoogleTasksRepository = mock()
    private val prefs: Prefs = mock()

    private val sync = Sync(listDao, taskDao, remoteRepo, prefs)
    private val baseModifiedTime = 1577836800000 // UTC Midnight 1/1/2020
    private val baseModifiedDate = Date(baseModifiedTime)
    private val defaultList = TaskList("1","Default").apply { isDefault = true; updated = baseModifiedDate }
    private val defaultListGoogle = GoogleTaskList("1","Default", baseModifiedDate, true)

    @BeforeEach
    fun beforeEachTest() {
        whenever(listDao.getAll()).thenReturn(listOf(defaultList))
        whenever(remoteRepo.getLists()).thenReturn(listOf(defaultListGoogle))

        // Default successful response
        whenever(remoteRepo.createList(any())).thenAnswer { (it.arguments[0] as TaskList).toGoogleList() }
        whenever(remoteRepo.updateList(any())).thenAnswer { (it.arguments[0] as TaskList).toGoogleList() }
        whenever(remoteRepo.deleteList(any())).thenReturn(true)
    }

    @Test
    fun `happy path has no changes`() {
        val result = sync.lists()

        assertTrue(result.success)
        assertEquals(0, result.totalChanges)
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `minimal data calls when no changes`() {
        sync.lists()

        // Get local data to check for changes
        assertEquals(1, Mockito.mockingDetails(listDao).invocations.size)
        //assertEquals(1, Mockito.mockingDetails(taskDao).invocations.size)
        // Remote data for changes
        assertEquals(1, Mockito.mockingDetails(remoteRepo).invocations.size)

        // TODO may not be needed after refactor
        // Save last sync time
        assertEquals(0, Mockito.mockingDetails(prefs).invocations.size)
    }

    @Test
    fun `initial sync pairs up default list`() {
        whenever(listDao.getAll()).thenReturn(listOf(TaskList("temp_123","Default List").apply { isDefault = true; updated = baseModifiedDate }))
        whenever(remoteRepo.getLists()).thenReturn(listOf(GoogleTaskList("1","My Tasks", baseModifiedDate, true)))

        val result = sync.lists()
        verify(listDao, times(1)).updateId(eq("temp_123"), eq("1"))
        assertEquals(0, result.totalChanges)
    }

    @Test
    fun `lists local to remote add+change`() {
        whenever(listDao.getAll()).thenReturn(listOf(
                defaultList.fullCopy().apply { title = "Default Modified"; isRenamed = true; updated = Date(baseModifiedTime + 1) },
                TaskList("temp_id", "Another list")
        ))

        val result = sync.lists()
        assertEquals(2, result.totalChanges)
        assertEquals(1, result.toRemote.add)
        assertEquals(1, result.toRemote.change)
        // Deleted is a special case depending on if tasks exist

        result.toLocal.change++ // offset call to local db to clear isRenamed
        verifyCallsMatchChanges(result)
    }

    @Test
    fun `lists local to remote delete`() {
        whenever(listDao.getAll()).thenReturn(listOf(defaultList, TaskList("2", "List B").apply { deleted = true }))
        whenever(remoteRepo.getLists()).thenReturn(listOf(defaultListGoogle, GoogleTaskList("2", "List B")))
        whenever(taskDao.getAllByList(eq("2"))).thenReturn(listOf(Task("2")))

        // Nothing if list is non-empty
        var result = sync.lists()
        assertEquals(0, result.totalChanges)

        // Deletes if empty
        whenever(taskDao.getAllByList(eq("2"))).thenReturn(emptyList())
        result = sync.lists()
        assertEquals(1, result.totalChanges)
        assertEquals(1, result.toRemote.delete)
    }

    @Test
    fun `lists remote to local`() {
        whenever(listDao.getAll()).thenReturn(listOf(
                defaultList,
                TaskList("3", "List not on remote")
        ))
        whenever(remoteRepo.getLists()).thenReturn(listOf(
                defaultListGoogle.copy(title = "New Title", updated = Date(baseModifiedTime + 1)),
                GoogleTaskList("2", "List Added")
        ))

        val result = sync.lists()

        assertEquals(3, result.totalChanges)
        assertEquals(1, result.toLocal.add)
        assertEquals(1, result.toLocal.change)
        assertEquals(1, result.toLocal.delete)
        verifyCallsMatchChanges(result)
    }

    private fun verifyCallsMatchChanges(result: ListSyncResult) {
        verify(remoteRepo, times(result.toRemote.add)).createList(any())
        verify(remoteRepo, times(result.toRemote.change)).updateList(any())
        verify(remoteRepo, times(result.toRemote.delete)).deleteList(any())

        verify(listDao, times(result.toLocal.add)).add(any())
        verify(listDao, times(result.toLocal.change)).update(any())
        verify(listDao, times(result.toLocal.delete)).delete(any())
    }

}
