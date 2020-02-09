package org.cerion.tasklist.database

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import org.cerion.tasklist.R
import java.util.*

enum class Theme {
    System,
    Light,
    Dark
}

class Prefs private constructor(private val context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    private val backgroundKey: String
        get() = context.getString(R.string.pref_key_background)

    var theme: Theme
        get() = Theme.values()[prefs.getInt(backgroundKey, 0)]
        set(theme) {
            setInt(backgroundKey, theme.ordinal)
        }

    private fun setInt(key: String, value: Int): Prefs {
        Log.d(TAG, "save $key $value")
        prefs.edit().putInt(key, value).apply()
        return this
    }

    fun setString(key: String, value: String): Prefs {
        Log.d(TAG, "save $key $value")
        prefs.edit().putString(key, value).apply()
        return this
    }

    fun setDate(key: String, value: Date): Prefs {
        Log.d(TAG, "save $key $value")
        prefs.edit().putLong(key, value.time).apply()

        return this
    }

    fun remove(key: String): Prefs {
        val editor = prefs.edit()
        editor.remove(key)
        editor.apply()

        return this
    }

    fun getString(key: String): String? {
        return prefs.getString(key, "")
    }

    fun getDate(key: String): Date {
        val lDate = prefs.getLong(key, 0)
        val date = Date()
        date.time = lDate
        return date
    }

    fun log() {
        Log.d(TAG, "--- Prefs ---")
        val keys = prefs.all

        for ((key, value) in keys) {
            Log.d("map values", key + ": " + value.toString())
        }
    }

    companion object {
        private val TAG = "prefs"

        const val KEY_LAST_LIST_SYNC = "lastListSync"
        @Deprecated("last sync requires context")
        const val KEY_LAST_SYNC = "lastSync"
        const val KEY_ACCOUNT_NAME = "accountName"
        const val KEY_LAST_SELECTED_LIST_ID = "lastListId"
        const val KEY_AUTHTOKEN = "authToken"
        const val KEY_AUTHTOKEN_DATE = "authTokenDate"

        @SuppressLint("StaticFieldLeak")
        private var instance: Prefs? = null
        private val LOCK = Any()

        fun getInstance(context: Context): Prefs {
            if (instance == null) {
                synchronized(LOCK) {
                    if (instance == null) {
                        instance = Prefs(context.applicationContext)
                    }
                }
            }

            instance?.log()
            return instance!!
        }
    }
}
