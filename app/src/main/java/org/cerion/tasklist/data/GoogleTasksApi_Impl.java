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

import androidx.annotation.Nullable;

public class GoogleTasksApi_Impl extends GoogleApiBase implements GoogleTasksApi {

    private static final String TAG = GoogleTasksApi_Impl.class.getSimpleName();

    GoogleTasksApi_Impl(String authKey) {
        super(authKey, GoogleApi.TASKS_BASE_URL);
    }

    private static final String FIELD_ID = "id";
    private static final String FIELD_TITLE = "title";
    private static final String FIELD_UPDATED = "updated";
    private static final String FIELD_NOTES = "notes";
    private static final String FIELD_DUE = "due";
    private static final String FIELD_DELETED = "deleted";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_COMPLETED = "completed";

    @Override
    public boolean delete(@NotNull Task task)
    {
        String sURL = getURL("lists/" + task.getListId() + "/tasks/" + task.getId());
        String result = getInetData(sURL,null, DELETE);

        return (result.length() == 0); //Successful delete does not return anything
    }

    @Override
    public Task insert(@NotNull Task task) throws GoogleApiException {
        String listId = task.getListId();
        //String listId = "@default";
        //if(task.getListId() != null && task.getListId().length() > 0)
        //    listId = task.getListId();

        String sURL = getURL("lists/" + listId + "/tasks");
        JSONObject json = new JSONObject();

        try
        {
            json.put(FIELD_TITLE, task.getTitle());
            json.put(FIELD_NOTES, task.getNotes());
            //Only need to set these if non-default value
            if(task.getHasDueDate())
                json.put(FIELD_DUE, mDateFormat.format(task.getDue()));
            if(task.getCompleted())
                json.put(FIELD_STATUS, "completed");

            JSONObject item = getJSON(sURL,json, POST);
            if(item != null && item.has(FIELD_ID) && item.has("selfLink")) {
                String newId = item.getString(FIELD_ID);

                String tokens[] = item.getString("selfLink").split("/");
                for(int i = 0; i < tokens.length - 1; i++) {
                    String token = tokens[i];
                    if(token.contentEquals("lists")) {
                        String newListId = tokens[i+1];
                        return new Task(newListId,newId);
                    }
                }
            }
        }
        catch (JSONException e)
        {
            Log.e(TAG,"exception",e);
        }

        return null;
    }

    @Override
    public boolean update(@NotNull Task task) throws GoogleApiException {
        String sURL = getURL("lists/" + task.getListId() + "/tasks/" + task.getId());
        JSONObject json = new JSONObject();
        boolean bResult = false;

        try
        {
            json.put(FIELD_ID, task.getId());
            json.put(FIELD_TITLE, task.getTitle());
            json.put(FIELD_NOTES, task.getNotes());
            json.put(FIELD_STATUS, task.getCompleted() ? "completed" : "needsAction" );

            if(!task.getCompleted())
                json.put(FIELD_COMPLETED, JSONObject.NULL);
            if(task.getHasDueDate())
                json.put(FIELD_DUE, mDateFormat.format(task.getDue()));
            else
                json.put(FIELD_DUE, JSONObject.NULL);

            JSONObject result = getJSON(sURL,json, PATCH);
            if(result != null && task.getId().contentEquals( result.getString("id") ))
                bResult = true;
        }
        catch (JSONException e)
        {
            Log.e(TAG,"",e);
        }

        return bResult;
    }

    @Override
    public List<Task> list(@NotNull String listId, @Nullable Date dtUpdatedMin) throws GoogleApiException {
        String sURL = getURL("lists/" + listId + "/tasks");
        if(dtUpdatedMin != null)
        {
            sURL += "&updatedMin=" + mDateFormat.format(dtUpdatedMin);
            sURL += "&showDeleted=true";
        }

        JSONObject json = getJSON(sURL);
        ArrayList<Task> result = new ArrayList<>();

        if(json != null)
        {
            try
            {
                JSONArray items = null;
                int count = 0;
                if(json.has("items"))
                {
                    items = json.getJSONArray("items");
                    count = items.length();
                }
                Log.d(TAG, count + " tasks");

                for(int i = 0; i < count; i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    Task task = parseItem(item,listId);
                    if(task != null)
                        result.add(task);
                }
            }
            catch (JSONException e) {
                Log.e(TAG,"exception",e);
            }

        }

        return result;
    }

    private Task parseItem(JSONObject item, String listId) throws JSONException {
        try {
            Task task = new Task(listId, item.getString(FIELD_ID));
            task.setTitle(item.getString(FIELD_TITLE));
            task.setUpdated(mDateFormat.parse(item.getString(FIELD_UPDATED)));

            if(item.has(FIELD_NOTES))
                task.setNotes(item.getString(FIELD_NOTES));
            task.setCompleted(item.getString(FIELD_STATUS).equals("completed"));

            if(item.has(FIELD_DUE)) {
                String sDue = item.getString(FIELD_DUE);
                task.setDue(mDateFormat.parse(item.getString(FIELD_DUE)) );
                System.out.println(sDue + " = " + task.getDue());
            }

            if(item.has(FIELD_DELETED))
                task.setDeleted(item.getBoolean(FIELD_DELETED));

            return task;
        }
        catch (ParseException e) {
            Log.e(TAG,"exception",e);
        }

        return null;
    }
}