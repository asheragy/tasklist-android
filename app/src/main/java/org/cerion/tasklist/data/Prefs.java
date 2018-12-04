package org.cerion.tasklist.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import org.cerion.tasklist.R;

import java.util.Date;
import java.util.Map;

public class Prefs
{
    private static final String TAG = "prefs";

    public static final String KEY_LAST_SYNC = "lastSync";
    public static final String KEY_ACCOUNT_NAME = "accountName";
    public static final String KEY_LAST_SELECTED_LIST_ID = "lastListId";
    public static final String KEY_AUTHTOKEN = "authToken";
    public static final String KEY_AUTHTOKEN_DATE = "authTokenDate";

    private static Prefs instance;
    private final SharedPreferences mPrefs;
    private final Context mContext;
    private static Object LOCK = new Object();

    private Prefs(Context context) {
        mContext = context;
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static Prefs getInstance(Context context) {
        if (instance == null) {
            synchronized(LOCK) {
                if (instance == null) {
                    instance = new Prefs(context);
                }
            }
        }

        return instance;
    }

    public Prefs setString(String key, String value)
    {
        Log.d(TAG,"save " + key + " " + value);
        mPrefs.edit().putString(key,value).apply();

        return this;
    }

    public Prefs setBool(String key, boolean value)
    {
        Log.d(TAG,"save " + key + " " + value);
        mPrefs.edit().putBoolean(key, value).apply();

        return this;
    }

    public Prefs setDate(String key, Date value)
    {
        Log.d(TAG,"save " + key + " " + value);
        mPrefs.edit().putLong(key, value.getTime()).apply();

        return this;
    }

    public Prefs remove(String key)
    {
        SharedPreferences.Editor editor = mPrefs.edit();
        editor.remove(key);
        editor.apply();

        return this;
    }


    public String getString(String key)
    {
        return mPrefs.getString(key,"");
    }

    public boolean getBool(String key)
    {
        return mPrefs.getBoolean(key,false);
    }

    public Date getDate(String key)
    {
        long lDate = mPrefs.getLong(key,0);
        Date date = new Date();
        date.setTime(lDate);
        return date;
    }

    public void log()
    {
        Log.d(TAG,"--- Prefs ---");
        Map<String,?> keys = mPrefs.getAll();

        for(Map.Entry<String,?> entry : keys.entrySet()){
            Log.d("map values",entry.getKey() + ": " +
                    entry.getValue().toString());
        }
    }

    public boolean isDarkTheme() {
        return mPrefs.getString(mContext.getString(R.string.pref_key_background),"0").contentEquals("1");
    }
}
