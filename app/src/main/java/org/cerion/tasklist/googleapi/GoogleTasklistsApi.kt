package org.cerion.tasklist.googleapi


import android.util.Log
import org.cerion.tasklist.common.TAG
import org.json.JSONException
import org.json.JSONObject
import java.util.*

data class GoogleTaskList(val id: String, val title: String, val updated: Date = Date(0), val default: Boolean = false)

class GoogleTasklistsApi internal constructor(authKey: String) : GoogleApiBase(authKey, GoogleApi.TASKS_BASE_URL) {

    fun delete(id: String): Boolean {
        val sURL = getURL("users/@me/lists/$id")
        val result = getInetData(sURL, null, DELETE)

        return result.isEmpty() //Successful delete does not return anything
    }

    operator fun get(id: String): GoogleTaskList? {
        val sURL = getURL("users/@me/lists/$id")
        val json = getJSON(sURL)

        try {
            return parseItem(json)
        } catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return null
    }

    fun getAll(): List<GoogleTaskList> {
        val sURL = getURL("users/@me/lists") + "&fields=items/id,items/title,items/updated"
        val json = getJSON(sURL)
        val result = ArrayList<GoogleTaskList>()

        try {
            val items = json.getJSONArray("items")
            Log.d(TAG, items.length().toString() + " task lists")

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val list = parseItem(item, i == 0) // first list is default list
                result.add(list)
            }
        }
        catch (e: JSONException) {
            Log.e(TAG, "exception", e)
        }

        return result
    }

    fun update(list: GoogleTaskList): GoogleTaskList {
        val sURL = getURL("users/@me/lists/" + list.id)
        val json = JSONObject()

        json.put(FIELD_ID, list.id)
        json.put(FIELD_TITLE, list.title)

        val result = getJSON(sURL, json, PUT)
        return parseItem(result)
    }

    fun insert(list: GoogleTaskList): GoogleTaskList {
        val sURL = getURL("users/@me/lists")
        val json = JSONObject()

        json.put(FIELD_TITLE, list.title)
        val item = getJSON(sURL, json, POST)
        return parseItem(item)
    }

    private fun parseItem(item: JSONObject, defaultList: Boolean = false): GoogleTaskList {
        val id = item.getString(FIELD_ID)
        val title = item.getString(FIELD_TITLE)
        val updated = item.getString(FIELD_UPDATED)

        val dt = dateFormat.parse(updated)
        return GoogleTaskList(id, title, dt, defaultList)
    }

    companion object {
        private const val FIELD_ID = "id"
        private const val FIELD_TITLE = "title"
        private const val FIELD_UPDATED = "updated"
    }
}