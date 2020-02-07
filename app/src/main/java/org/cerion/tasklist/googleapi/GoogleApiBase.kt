package org.cerion.tasklist.googleapi

import android.util.Log
import com.squareup.okhttp.MediaType
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request
import com.squareup.okhttp.RequestBody
import org.cerion.tasklist.common.TAG
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

open class GoogleApiBase internal constructor(private val mAuthKey: String, private val mBaseUrl: String) {

    internal val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }

    @Throws(GoogleApiException::class)
    internal fun getJSON(sURL: String, requestBody: JSONObject? = null, method: Int = GET): JSONObject {
        var sRequestBody: String? = null
        if (requestBody != null)
            sRequestBody = requestBody.toString()

        val sData = getInetData(sURL, sRequestBody, method)

        var json = JSONObject(sData)
        if (json.has("error")) {
            json = json.getJSONObject("error")
            val code = json.getInt("code")
            val message = json.getString("message")

            throw GoogleApiException(message, code)
        }

        return json
    }

    internal fun getURL(endpoint: String): String {
        return mBaseUrl + endpoint + "?key=" + GoogleApi.API_KEY
    }

    // TODO change to return struct with response code too
    internal fun getInetData(sURL: String, sRequestBody: String?, method: Int): String {
        val command =
        when (method) {
            POST -> "POST"
            PUT -> "PUT"
            PATCH -> "PATCH"
            DELETE -> "DELETE"
            else -> "GET"
        }

        Log.d(TAG, "$command=$sURL")

        if (sRequestBody != null)
            Log.d(TAG, "Body=$sRequestBody")

        var sResult = ""

        val client = OkHttpClient()
        client.setReadTimeout(3, TimeUnit.SECONDS)
        client.setWriteTimeout(3, TimeUnit.SECONDS)

        var requestBuilder: Request.Builder = Request.Builder()
                .url(sURL)
                .addHeader("Authorization", "Bearer $mAuthKey")

        //connection.setRequestProperty("Content-type", "application/json");

        if (sRequestBody != null) {
            val json = MediaType.parse("application/json; charset=utf-8")
            val body = RequestBody.create(json, sRequestBody)

            when (method) {
                POST -> requestBuilder = requestBuilder.post(body)
                PUT -> requestBuilder = requestBuilder.put(body)
                PATCH -> requestBuilder = requestBuilder.patch(body)
            }
        }
        else if (method == DELETE)
            requestBuilder = requestBuilder.delete()
        else
            requestBuilder = requestBuilder.get()

        val request = requestBuilder.build()

        try {
            val response = client
                    .newCall(request)
                    .execute()

            Log.d(TAG, "Result=" + response.code())
            sResult = response.body().string()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        if (GoogleApi.mLogFullResults)
            Log.d(TAG, "Body=$sResult")
        return sResult
    }

    companion object {
        const val GET = 0
        const val PUT = 1
        const val POST = 2
        const val PATCH = 3
        const val DELETE = 4
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
