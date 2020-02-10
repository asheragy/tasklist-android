package org.cerion.tasklist.googleapi


import android.util.Log
import androidx.annotation.Nullable
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.Task
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class GoogleTasksApi internal constructor(authKey: String) : GoogleApiBase(authKey, GoogleApi.TASKS_BASE_URL) {

    fun delete(task: Task): Boolean {
        val sURL = getURL("lists/" + task.listId + "/tasks/" + task.id)
        val result = getInetData(sURL, null, DELETE)

        return result.isEmpty() //Successful delete does not return anything
    }

    fun insert(task: Task): Task {
        val listId = task.listId
        val sURL = getURL("lists/$listId/tasks")

        val json = JSONObject()
        json.put(FIELD_TITLE, task.title)
        json.put(FIELD_NOTES, task.notes)

        //Only need to set these if non-default value
        if (task.hasDueDate)
            json.put(FIELD_DUE, dateFormat.format(task.due))
        if (task.completed)
            json.put(FIELD_STATUS, "completed")

        val item = getJSON(sURL, json, POST)
        return parseItem(item, listId)
    }

    fun update(task: Task): Task {
        val sURL = getURL("lists/" + task.listId + "/tasks/" + task.id)
        val json = JSONObject()

        json.put(FIELD_ID, task.id)
        json.put(FIELD_TITLE, task.title)
        json.put(FIELD_NOTES, task.notes)
        json.put(FIELD_STATUS, if (task.completed) "completed" else "needsAction")

        if (!task.completed)
            json.put(FIELD_COMPLETED, JSONObject.NULL)
        if (task.hasDueDate)
            json.put(FIELD_DUE, dateFormat.format(task.due))
        else
            json.put(FIELD_DUE, JSONObject.NULL)

        val result = getJSON(sURL, json, PATCH)

        if (!task.id.contentEquals(result.getString("id")))
            throw Exception("unexpected id")

        return parseItem(result, task.listId)
    }

    fun list(listId: String, @Nullable dtUpdatedMin: Date?): List<Task> {
        var sURL = getURL("lists/$listId/tasks")
        if (dtUpdatedMin != null) {
            sURL += "&updatedMin=" + dateFormat.format(dtUpdatedMin)
            sURL += "&showDeleted=true"
        }

        //sURL += "&fields=items/id,items/title,items/updated,items/notes,items/due,items/deleted,items/status,items/completed"

        val json = getJSON(sURL)
        val result = ArrayList<Task>()

        var items: JSONArray? = null
        var count = 0
        if (json.has("items")) {
            items = json.getJSONArray("items")
            count = items!!.length()
        }
        Log.d(TAG, count.toString() + " tasks")

        for (i in 0 until count) {
            val item = items!!.getJSONObject(i)
            val task = parseItem(item, listId)
            result.add(task)
        }

        return result
    }

    private fun parseItem(item: JSONObject, listId: String): Task {
        return Task(listId).apply {
            id = item.getString(FIELD_ID)
            title = item.getString(FIELD_TITLE)
            updated = dateFormat.parse(item.getString(FIELD_UPDATED))
            completed = item.getString(FIELD_STATUS) == "completed"

            if (item.has(FIELD_NOTES))
                notes = item.getString(FIELD_NOTES)

            if (item.has(FIELD_DUE))
                due = dateFormat.parse(item.getString(FIELD_DUE))

            if (item.has(FIELD_DELETED))
                deleted = item.getBoolean(FIELD_DELETED)

            val selfLink = item.getString("selfLink")
            if (!selfLink.contains(listId))
                throw IllegalStateException("selfLink does not match listId")
        }
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_UPDATED = "updated"
        private const val FIELD_NOTES = "notes"
        private const val FIELD_DUE = "due"
        private const val FIELD_DELETED = "deleted"
        private const val FIELD_STATUS = "status"
        private const val FIELD_COMPLETED = "completed"
    }
}