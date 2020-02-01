package org.cerion.tasklist.googleapi


import android.util.Log
import org.cerion.tasklist.common.TAG
import org.cerion.tasklist.database.TaskList
import org.json.JSONException
import org.json.JSONObject
import java.text.ParseException
import java.util.*

class GoogleTasklistsApi internal constructor(authKey: String) : GoogleApiBase(authKey, GoogleApi.TASKS_BASE_URL) {

    fun delete(list: TaskList): Boolean {
        val sURL = getURL("users/@me/lists/" + list.id)
        val result = getInetData(sURL, null, DELETE)

        return result.isEmpty() //Successful delete does not return anything
    }

    operator fun get(id: String): TaskList? {
        val sURL = getURL("users/@me/lists/$id")
        val json = getJSON(sURL)

        try {
            return parseItem(json)
        } catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return null
    }

    fun getAll(): List<TaskList> {
        val sURL = getURL("users/@me/lists")
        val json = getJSON(sURL)
        val result = ArrayList<TaskList>()

        try {

            val items = json.getJSONArray("items")
            Log.d(TAG, items.length().toString() + " task lists")

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val list = parseItem(item)
                if (list != null) {
                    if (i == 0)
                        list.isDefault = true //first list is default list
                    result.add(list)
                }

            }
        } catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return result
    }

    fun update(list: TaskList): Boolean {
        val sURL = getURL("users/@me/lists/" + list.id)
        val json = JSONObject()
        var bResult = false

        try {
            json.put(FIELD_ID, list.id)
            json.put(FIELD_TITLE, list.title)

            val result = getJSON(sURL, json, PUT)
            if (list.title.contentEquals(result.getString(FIELD_TITLE)))
                bResult = true
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return bResult
    }

    fun insert(list: TaskList): TaskList? {
        val sURL = getURL("users/@me/lists")
        val json = JSONObject()
        var result: TaskList? = null

        try {
            json.put(FIELD_TITLE, list.title)
            val item = getJSON(sURL, json, POST)
            result = parseItem(item)

        } catch (e: JSONException) {
            e.printStackTrace()
        }

        return result
    }

    @Throws(JSONException::class)
    private fun parseItem(item: JSONObject): TaskList? {
        var result: TaskList? = null
        val id = item.getString(FIELD_ID)
        val title = item.getString(FIELD_TITLE)
        val updated = item.getString(FIELD_UPDATED)
        try {
            val dt = dateFormat.parse(updated)
            result = TaskList(id, title)
            result.updated = dt
        } catch (e: ParseException) {
            Log.e(TAG, "exception", e)
        }

        return result
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_UPDATED = "updated"
    }
}