package org.cerion.tasklist.data;

import android.util.Log;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

class GoogleApiBase {

    private static final String TAG = GoogleApiBase.class.getSimpleName();

    private final String mAuthKey;
    private final String mBaseUrl;

    final SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    private static final int GET    = 0;
    static final int PUT    = 1;
    static final int POST   = 2;
    static final int PATCH  = 3;
    static final int DELETE = 4;

    GoogleApiBase(String authKey, String baseUrl) {
        mDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        mAuthKey = authKey;
        mBaseUrl = baseUrl;
    }

    JSONObject getJSON(String sURL) throws GoogleApiException {
        return getJSON(sURL, null, GET);
    }

    JSONObject getJSON(String sURL, JSONObject requestBody, int method) throws GoogleApiException {
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

                throw new GoogleApiException(message,code);
            }
        }
        catch (JSONException e)
        {
            Log.d(TAG, "exception", e);
        }

        return json;
    }

    String getURL(String endpoint) {
        return mBaseUrl + endpoint + "?key=" + GoogleApi.API_KEY;
    }

    String getInetData(String sURL, String sRequestBody, int method) {
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

        if(GoogleApi.mLogFullResults)
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
