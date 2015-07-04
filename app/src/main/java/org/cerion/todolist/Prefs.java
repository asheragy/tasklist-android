package org.cerion.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.Date;

public class Prefs
{
    public static final String TAG = "prefs";
    public static final String PREF_AUTHTOKEN = "authToken";
    public static final String PREF_AUTHTOKEN_DATE = "authTokenDate";

    public static void savePref(Context context, String key, String value)
    {
        Log.d(TAG,"save " + key + " " + value);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key,value);
        editor.apply();
    }

    public static void savePrefDate(Context context, String key, Date value)
    {
        Log.d(TAG,"save " + key + " " + value);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(key, value.getTime());
        editor.apply();
    }


    public static String getPref(Context context, String key)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(key,"");
    }

    public static Date getPrefDate(Context context, String key)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long lDate = prefs.getLong(key,0);
        Date date = new Date();
        date.setTime(lDate);
        return date;
    }
}
