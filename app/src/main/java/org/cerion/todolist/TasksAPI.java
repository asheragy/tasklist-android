package org.cerion.todolist;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class TasksAPI
{
    private static final String TAG = "TasksAPI";
    private static final String API_KEY = "AIzaSyAHgDGorXJ1I0WqzpWE6Pm2o894T-j4pdQ";
    private static final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private static final boolean mLogFullResults = true;

    public String mAuthKey;

    public TasksAPI(String authKey)
    {
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mAuthKey = authKey;
    }

    public boolean deleteTask(Task task)
    {
        String sURL = "https://www.googleapis.com/tasks/v1/lists/" + task.listId + "/tasks/" + task.id + "?key=" + API_KEY;
        String result = getInetData(sURL,null,"DELETE");
        Log.d(TAG, "deleteTask = " + result);
        //TODO, detect failure, if task does not exist that should return success too
        return true;
    }

    public boolean updateTask(Task task)
    {
        String sURL = "https://www.googleapis.com/tasks/v1/lists/" + task.listId + "/tasks/" + task.id + "?key=" + API_KEY;
        JSONObject json = new JSONObject();
        boolean bResult = false;

        try
        {
            json.put("id", task.id);
            json.put("title", task.title);

            JSONObject result = getJSON(sURL,json,"PATCH");
            if(result != null && task.id.contentEquals( result.getString("id") ))
                bResult = true;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return bResult;
    }


    public ArrayList<Task> getTasks(String listId, Date dtUpdatedMin)
    {
        String sURL = "https://www.googleapis.com/tasks/v1/lists/" + listId + "/tasks?key=" + API_KEY;
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
                    //String updated = item.getString("updated");
                    try
                    {
                        Task task = new Task(listId, item.getString("id"));
                        task.title = item.getString("title");
                        task.updated = mDateFormat.parse(item.getString("updated"));

                        if(item.has("notes"))
                            task.notes = item.getString("notes");
                        else
                            task.notes = "";
                        task.completed = (item.getString("status").equals("completed"));

                        if(item.has("due"))
                        {
                            String sDue = item.getString("due");
                            task.due = mDateFormat.parse(item.getString("due"));
                            System.out.println(sDue + " = " + task.due);
                        }

                        if(item.has("deleted"))
                            task.deleted = item.getBoolean("deleted");

                        result.add(task);
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                    }

                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

        }

        return result;
    }


    private TaskList getTaskList(JSONObject item)
    {
        try
        {
            String updated = item.getString("updated");
            try
            {
                Date dt = mDateFormat.parse(updated);
                TaskList list = new TaskList(item.getString("id"), item.getString("title"));
                list.updated = dt;
                return list;
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public ArrayList<TaskList> getTaskLists()
    {
        String sURL = "https://www.googleapis.com/tasks/v1/users/@me/lists?key=" + API_KEY;
        JSONObject json = getJSON(sURL);
        ArrayList<TaskList> result = new ArrayList<>();

        if(json != null)
        {
            try
            {
                JSONArray items = json.getJSONArray("items");
                Log.d(TAG,items.length() + " task lists");

                for(int i = 0; i < items.length(); i++)
                {
                    JSONObject item = items.getJSONObject(i);
                    TaskList list = getTaskList(item);
                    if(list != null)
                        result.add(list);
                    else
                        Log.d(TAG,"Failed to parse task");
                    /*
                    String updated = item.getString("updated");
                    try
                    {
                        //SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

                        Date dt = mDateFormat.parse(updated);
                        TaskList list = new TaskList(item.getString("id"), item.getString("title"));
                        list.updated = dt;
                        result.add(list);
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                    }
                    */

                }
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }

        }

        return result;
    }

    public boolean updateTaskList(TaskList list)
    {
        String sURL = "https://www.googleapis.com/tasks/v1/users/@me/lists/" + list.id + "?key=" + API_KEY;
        JSONObject json = new JSONObject();
        boolean bResult = false;

        try
        {
            json.put("id", list.id);
            json.put("title", list.title);

            JSONObject result = getJSON(sURL,json,"PUT");
            if(result != null && list.title.contentEquals( result.getString("title") ))
                bResult = true;
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return bResult;
    }

    public TaskList addTaskList(TaskList list)
    {
        String sURL = "https://www.googleapis.com/tasks/v1/users/@me/lists?key=" + API_KEY;
        JSONObject json = new JSONObject();
        TaskList result = null;

        try
        {
            json.put("title", list.title);

            JSONObject item = getJSON(sURL,json,"POST");
            if(item != null)
                result = getTaskList(item);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return result;
    }

    private JSONObject getJSON(String sURL)
    {
        return getJSON(sURL, null, null);
    }

    private JSONObject getJSON(String sURL, JSONObject requestBody, String sRequestMethod)
    {
        String sRequestBody = null;
        if(requestBody != null)
            sRequestBody = requestBody.toString();

        String sData = getInetData(sURL,sRequestBody,sRequestMethod);
        JSONObject json = null;

        try
        {
            json = new JSONObject(sData);
        }
        catch (JSONException e)
        {
            e.printStackTrace();
        }

        return json;
    }

    private String getInetData(String sURL, String sRequestBody, String sRequestMethod)
    {
        Log.d(TAG,"Command=" + sURL);
        String sResult = "";
        URL url;
        try
        {
            url = new URL(sURL);
        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
            return null;
        }

        HttpURLConnection connection;
        try
        {
            connection = (HttpURLConnection) url.openConnection();

            if (sRequestMethod != null && !sRequestMethod.equals("PATCH")) //Does not support patch here
                connection.setRequestMethod(sRequestMethod);

            if (sRequestBody != null)
            {
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setRequestProperty("Content-Length", Integer.toString(sRequestBody.getBytes().length));
            }
            connection.setRequestProperty("Content-type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer " + mAuthKey);

            //Workaround for PATCH command
            if(sRequestMethod != null && sRequestMethod.equals("PATCH"))
                connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");

            connection.connect();

            if (sRequestBody != null)
            {
                DataOutputStream wr = new DataOutputStream( connection.getOutputStream());
                wr.write( sRequestBody.getBytes() );
                wr.flush();
                //wr.close();

            }

            Log.d(TAG,"Result="+connection.getResponseCode());
            //if(true)//urlConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
            {
                InputStream is = connection.getInputStream();
                InputStream in = new BufferedInputStream(is);


                byte[] contents = new byte[1024];

                int bytesRead;
                while( (bytesRead = in.read(contents)) != -1)
                    sResult += new String(contents, 0, bytesRead);

                connection.disconnect();
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        if(mLogFullResults)
            Log.d(TAG,"Data=" + sResult);
        return sResult;
    }

}
