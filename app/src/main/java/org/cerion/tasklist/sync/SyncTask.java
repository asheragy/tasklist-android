package org.cerion.tasklist.sync;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.cerion.tasklist.data.GoogleApiException;
import org.cerion.tasklist.data.Prefs;

import java.net.HttpURLConnection;
import java.util.Date;

public class SyncTask extends AsyncTask<Void, Void, Boolean> {

    private static final String TAG = SyncTask.class.getSimpleName();
    private final Context mContext;
    private final String mAuthToken;
    private final OnSyncCompleteListener mCallback;

    private int mChanges = 0;
    private Exception mError = null;

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

    public SyncTask(Context context, String sAuth, OnSyncCompleteListener callback) {
        mContext = context;
        mAuthToken = sAuth;
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        Sync sync = Sync.getInstance(mContext,mAuthToken);
        boolean result;

        try {
            result = sync.run();
        } catch (GoogleApiException e) {
            Log.d(TAG,"GoogleApiException " + e.getMessage());
            mError = e;
            result = false;

            //If unauthorized clear token so it will try to get a new one
            if(e.getErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                AuthTools.clearSavedToken(mContext);
        }

        for(int i = 0; i < sync.dbToGoogle.length; i++) {
            mChanges += sync.dbToGoogle[i];
            mChanges += sync.googleToDb[i];
        }

        return result;
    }

    @Override
    protected void onPostExecute(Boolean result)
    {
        Log.d(TAG, "Result=" + result + " Changes=" + mChanges);
        if(result)
            Prefs.getInstance(mContext).setDate(Prefs.KEY_LAST_SYNC, new Date());

        mCallback.onSyncFinish(result,mError);
    }
}
