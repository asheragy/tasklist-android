package org.cerion.tasklist.googleapi


import android.util.Log
import androidx.annotation.Nullable
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.Task
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.util.*

class GoogleTasksApi_Impl internal constructor(authKey: String) : GoogleApiBase(authKey, GoogleApi.TASKS_BASE_URL), GoogleTasksApi {

    override fun delete(task: Task): Boolean {
        val sURL = getURL("lists/" + task.listId + "/tasks/" + task.id)
        val result = getInetData(sURL, null, DELETE)

        return result.isEmpty() //Successful delete does not return anything
    }

    @Throws(GoogleApiException::class)
    override fun insert(task: Task): Task? {
        val listId = task.listId
        //String listId = "@default";
        //if(task.getListId() != null && task.getListId().length() > 0)
        //    listId = task.getListId();

        val sURL = getURL("lists/$listId/tasks")
        val json = JSONObject()

        try {
            json.put(FIELD_TITLE, task.title)
            json.put(FIELD_NOTES, task.notes)
            //Only need to set these if non-default value
            if (task.hasDueDate)
                json.put(FIELD_DUE, dateFormat.format(task.due))
            if (task.completed)
                json.put(FIELD_STATUS, "completed")

            val item = getJSON(sURL, json, POST)
            if (item.has(FIELD_ID) && item.has("selfLink")) {
                val newId = item.getString(FIELD_ID)

                val tokens = item.getString("selfLink").split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (i in 0 until tokens.size - 1) {
                    val token = tokens[i]
                    if (token.contentEquals("lists")) {
                        val newListId = tokens[i + 1]
                        return Task(newListId).apply {
                            id = newId
                        }
                    }
                }
            }
        } catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return null
    }

    @Throws(GoogleApiException::class)
    override fun update(task: Task): Boolean {
        val sURL = getURL("lists/" + task.listId + "/tasks/" + task.id)
        val json = JSONObject()
        var bResult = false

        try {
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
            if (task.id.contentEquals(result.getString("id")))
                bResult = true
        } catch (e: JSONException) {
            Log.e(TAG, "", e)
        }

        return bResult
    }

    @Throws(GoogleApiException::class)
    override fun list(listId: String, @Nullable dtUpdatedMin: Date?): List<Task>? {
        var sURL = getURL("lists/$listId/tasks")
        if (dtUpdatedMin != null) {
            sURL += "&updatedMin=" + dateFormat.format(dtUpdatedMin)
            sURL += "&showDeleted=true"
        }

        val json = getJSON(sURL)
        val result = ArrayList<Task>()

        try {
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
                if (task != null)
                    result.add(task)
            }
        } catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return result
    }

    @Throws(JSONException::class)
    private fun parseItem(item: JSONObject, listId: String): Task? {
        try {
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
            }
        }
        catch (e: ParseException) {
            Log.e(TAG, "exception", e)
        }

        return null
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