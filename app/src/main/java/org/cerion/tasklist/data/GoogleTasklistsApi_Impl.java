package org.cerion.tasklist.data;


import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class GoogleTasklistsApi_Impl extends GoogleApiBase implements GoogleTasklistsApi {

    private static final String TAG = GoogleTasklistsApi_Impl.class.getSimpleName();

    GoogleTasklistsApi_Impl(String authKey) {
        super(authKey, GoogleApi.TASKS_BASE_URL);
    }

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_UPDATED = "updated";

    @Override
    public TaskList get(@NotNull String id) throws GoogleApiException {
        String sURL = getURL("users/@me/lists/" + id);
        JSONObject json = getJSON(sURL);

        if(json != null) {
            try {
                return parseItem(json);
            }
            catch (JSONException e) {
                Log.e(TAG, "exception", e);
            }
        }

        return null;
    }

    @Override
    public List<TaskList> getAll() throws GoogleApiException {
        String sURL = getURL("users/@me/lists");
        JSONObject json = getJSON(sURL);
        ArrayList<TaskList> result = new ArrayList<>();

        if(json != null) {
            try {

                JSONArray items = json.getJSONArray("items");
                Log.d(TAG, items.length() + " task lists");

                for (int i = 0; i < items.length(); i++) {
                    JSONObject item = items.getJSONObject(i);
                    TaskList list = parseItem(item);
                    if (list != null) {
                        if(i==0)
                            list.setDefault(true); //first list is default list
                        result.add(list);
                    }

                }
            }

            catch (JSONException e) {
                Log.e(TAG, "exception", e);
            }

        }

        return result;
    }

    @Override
    public boolean update(@NotNull TaskList list) throws GoogleApiException {
        String sURL = getURL("users/@me/lists/" + list.getId());
        JSONObject json = new JSONObject();
        boolean bResult = false;

        try
        {
            json.put(FIELD_ID, list.getId());
            json.put(FIELD_TITLE, list.getTitle());

            JSONObject result = getJSON(sURL, json, PUT);
            if(result != null && list.getTitle().contentEquals( result.getString(FIELD_TITLE) ))
                bResult = true;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return bResult;
    }

    @Override
    public TaskList insert(@NotNull TaskList list) throws GoogleApiException {
        String sURL = getURL("users/@me/lists");
        JSONObject json = new JSONObject();
        TaskList result = null;

        try
        {
            json.put(FIELD_TITLE, list.getTitle());
            JSONObject item = getJSON(sURL, json, POST);
            if(item != null)
                result = parseItem(item);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private TaskList parseItem(JSONObject item) throws JSONException {
        TaskList result = null;
        String id = item.getString(FIELD_ID);
        String title = item.getString(FIELD_TITLE);
        String updated = item.getString(FIELD_UPDATED);
        try {
            Date dt = mDateFormat.parse(updated);
            result = new TaskList(id, title);
            result.setUpdated(dt);
        }
        catch (ParseException e) {
            Log.e(TAG, "exception", e);
        }

        return result;
    }
}