package org.cerion.tasklist.sync


import android.content.Context
import android.os.AsyncTask
import android.util.Log
import org.cerion.tasklist.data.GoogleApiException
import java.net.HttpURLConnection

class SyncTask(private val mContext: Context, private val mAuthToken: String, private val mCallback: OnSyncCompleteListener) : AsyncTask<Void, Void, Boolean>() {

    private var mChanges = 0
    private var mError: Exception? = null

    override fun doInBackground(vararg params: Void): Boolean? {
        val sync = Sync.getInstance(mContext, mAuthToken)
        var result: Boolean

        try {
            result = sync.run()
        } catch (e: GoogleApiException) {
            Log.d(TAG, "GoogleApiException " + e.message)
            mError = e
            result = false

            // TODO this needs to get moved elsewhere and maybe a few other things here
            //If unauthorized clear token so it will try to get a new one
            if (e.errorCode == HttpURLConnection.HTTP_UNAUTHORIZED)
                AuthTools.clearSavedToken(mContext)
        }

        for (i in sync.dbToGoogle.indices) {
            mChanges += sync.dbToGoogle[i]
            mChanges += sync.googleToDb[i]
        }

        return result
    }

    /**
     * Get token and run sync process in background
     * @param context Context
     * @param activity Use for permissions prompt if starting from activity
     * @param callback Listener for when sync completes
     */
/*
    public static void run(Context context, @Nullable Activity activity, OnSyncCompleteListener callback) {
        String token = AuthTools.getSavedToken(context);

        if(token != null) {
            SyncTask task = new SyncTask(context, token, callback);
            task.execute();
        } else {
            //Get new token then run sync
            AuthTools.getTokenAndSync(context, activity, callback);
        }
    }
    */

    override fun onPostExecute(result: Boolean) {
        mCallback.onSyncFinish(result, mError)
    }

    companion object {

        private val TAG = SyncTask::class.java.simpleName
    }
}
