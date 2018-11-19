package org.cerion.tasklist.data;

import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.cerion.tasklist.data.IGoogleTasksAPI.APIException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.Nullable;

public class GoogleTasksAPI
{
    private static final String TAG = GoogleTasksAPI.class.getSimpleName();

    private final String BASE_URL = "https://www.googleapis.com/tasks/v1/";
    private final String API_KEY = "346378052412-b0lrj3jgnucf299u3qf23c4sh4agdgsk.apps.googleusercontent.com";
    //private static final String API_KEY = "AIzaSyAHgDGorXJ1I0WqzpWE6Pm2o894T-j4pdQ";

    private static final boolean mLogFullResults = true;
    private static final int GET    = 0;
    private static final int PUT    = 1;
    private static final int POST   = 2;
    private static final int PATCH  = 3;
    private static final int DELETE = 4;

    private final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
    private final String mAuthKey;

    public final IGoogleTasksAPI.Tasklists taskLists;
    public final IGoogleTasksAPI.Tasks tasks;


    public GoogleTasksAPI(String authKey)
    {
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mAuthKey = authKey;
        taskLists = new TaskLists(this);
        tasks = new Tasks(this);
    }

    public static class TaskLists implements IGoogleTasksAPI.Tasklists {
        private GoogleTasksAPI parent = null;
        private TaskLists(GoogleTasksAPI api)
        {
            parent = api;
        }

        private static final String FIELD_ID = "id";
        private static final String FIELD_TITLE = "title";
        private static final String FIELD_UPDATED = "updated";

        @Override
        public TaskList get(String id) throws APIException {
            String sURL = parent.getURL("users/@me/lists/" + id);
            JSONObject json = parent.getJSON(sURL);

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
        public List<TaskList> list() throws APIException {
            String sURL = parent.getURL("users/@me/lists");
            JSONObject json = parent.getJSON(sURL);
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
        public boolean update(TaskList list) throws APIException {
            String sURL = parent.getURL("users/@me/lists/" + list.getId());
            JSONObject json = new JSONObject();
            boolean bResult = false;

            try
            {
                json.put(FIELD_ID, list.getId());
                json.put(FIELD_TITLE, list.getTitle());

                JSONObject result = parent.getJSON(sURL,json,PUT);
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
        public TaskList insert(TaskList list) throws IGoogleTasksAPI.APIException {
            String sURL = parent.getURL("users/@me/lists");
            JSONObject json = new JSONObject();
            TaskList result = null;

            try
            {
                json.put(FIELD_TITLE, list.getTitle());
                JSONObject item = parent.getJSON(sURL, json, POST);
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
                Date dt = parent.mDateFormat.parse(updated);
                result = new TaskList(id, title);
                result.setUpdated(dt);
            }
            catch (ParseException e) {
                Log.e(TAG, "exception", e);
            }

            return result;
        }
    }

    public static class Tasks implements IGoogleTasksAPI.Tasks {
        private GoogleTasksAPI parent = null;
        private Tasks(GoogleTasksAPI api) {
            parent = api;
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
        public boolean delete(Task task)
        {
            String sURL = parent.getURL("lists/" + task.listId + "/tasks/" + task.id);
            String result = parent.getInetData(sURL,null,DELETE);

            return (result.length() == 0); //Successful delete does not return anything
        }

        @Override
        public Task insert(Task task) throws APIException {
            String listId = "@default";
            if(task.listId != null && task.listId.length() > 0)
                listId = task.listId;

            String sURL = parent.getURL("lists/" + listId + "/tasks");
            JSONObject json = new JSONObject();

            try
            {
                json.put(FIELD_TITLE, task.title);
                json.put(FIELD_NOTES, task.notes);
                //Only need to set these if non-default value
                if(task.due.getTime() > 0)
                    json.put(FIELD_DUE, parent.mDateFormat.format(task.due));
                if(task.completed)
                    json.put(FIELD_STATUS, "completed");

                JSONObject item = parent.getJSON(sURL,json,POST);
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
        public boolean update(Task task) throws IGoogleTasksAPI.APIException {
            String sURL = parent.getURL("lists/" + task.listId + "/tasks/" + task.id);
            JSONObject json = new JSONObject();
            boolean bResult = false;

            try
            {
                json.put(FIELD_ID, task.id);
                json.put(FIELD_TITLE, task.title);
                json.put(FIELD_NOTES, task.notes);
                json.put(FIELD_STATUS, task.completed ? "completed" : "needsAction" );

                if(!task.completed)
                    json.put(FIELD_COMPLETED, JSONObject.NULL);
                if(task.due.getTime() > 0)
                    json.put(FIELD_DUE, parent.mDateFormat.format(task.due));
                else
                    json.put(FIELD_DUE, JSONObject.NULL);

                JSONObject result = parent.getJSON(sURL,json,PATCH);
                if(result != null && task.id.contentEquals( result.getString("id") ))
                    bResult = true;
            }
            catch (JSONException e)
            {
                Log.e(TAG,"",e);
            }

            return bResult;
        }

        @Override
        public List<Task> list(String listId, @Nullable Date dtUpdatedMin) throws APIException {
            String sURL = parent.getURL("lists/" + listId + "/tasks");
            if(dtUpdatedMin != null)
            {
                sURL += "&updatedMin=" + parent.mDateFormat.format(dtUpdatedMin);
                sURL += "&showDeleted=true";
            }

            JSONObject json = parent.getJSON(sURL);
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
                task.title = item.getString(FIELD_TITLE);
                task.updated = parent.mDateFormat.parse(item.getString(FIELD_UPDATED));

                if(item.has(FIELD_NOTES)) task.notes = item.getString(FIELD_NOTES);
                task.completed = (item.getString(FIELD_STATUS).equals("completed"));

                if(item.has(FIELD_DUE)) {
                    String sDue = item.getString(FIELD_DUE);
                    task.due = parent.mDateFormat.parse(item.getString(FIELD_DUE));
                    System.out.println(sDue + " = " + task.due);
                }
                else
                    task.due = new Date(0);

                if(item.has(FIELD_DELETED))
                    task.deleted = item.getBoolean(FIELD_DELETED);

                return task;
            }
            catch (ParseException e) {
                Log.e(TAG,"exception",e);
            }

            return null;
        }
    }

    private JSONObject getJSON(String sURL) throws APIException {
        return getJSON(sURL, null, GET);
    }

    private JSONObject getJSON(String sURL, JSONObject requestBody, int method) throws IGoogleTasksAPI.APIException {
        String sRequestBody = null;
        if(requestBody != null)
            sRequestBody = requestBody.toString();

        String sData = getInetData(sURL,sRequestBody,method);
        JSONObject json = null;

        try
        {
            json = new JSONObject(sData);

            if(json.has("error"))
            {
                json = json.getJSONObject("error");

                int code = json.getInt("code");
                String message = json.getString("message");

                throw new APIException(message,code);
            }
        }
        catch (JSONException e)
        {
            Log.d(TAG, "exception", e);
        }

        return json;
    }

    private String getURL(String endpoint) {
        return BASE_URL + endpoint + "?key=" + API_KEY;
    }

    private String getInetData(String sURL, String sRequestBody, int method)
    {
        String command = "GET";
        if(method == POST) command = "POST";
        else if(method == PUT) command = "PUT";
        else if(method == PATCH) command = "PATCH";
        else if(method == DELETE) command = "DELETE";
        Log.d(TAG,command + "=" + sURL);

        if(sRequestBody != null)
            Log.d(TAG,"Body=" + sRequestBody);

        String sResult = "";

        OkHttpClient client = new OkHttpClient();
        Request.Builder requestBuilder = new Request.Builder().url(sURL).addHeader("Authorization", "Bearer " + mAuthKey);
        //connection.setRequestProperty("Content-type", "application/json");

        if(sRequestBody != null)
        {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(JSON, sRequestBody);

            if(method == POST)
                requestBuilder = requestBuilder.post(body);
            else if(method == PUT)
                requestBuilder = requestBuilder.put(body);
            else if(method == PATCH)
                requestBuilder = requestBuilder.patch(body);
        }
        else if(method == DELETE)
            requestBuilder = requestBuilder.delete();
        else
            requestBuilder = requestBuilder.get();

        Request request = requestBuilder.build();

        try {
            Response response = client.newCall(request).execute();
            Log.d(TAG, "Result=" + response.code());
            sResult = response.body().string();

        } catch (IOException e) {
            e.printStackTrace();
        }

        if(mLogFullResults)
            Log.d(TAG,"Body=" + sResult);
        return sResult;
    }


    /* Without OkHttp
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
    */

}
