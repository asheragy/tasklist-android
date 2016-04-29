package org.cerion.tasklist.sync;


import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.cerion.tasklist.data.Prefs;
import org.cerion.tasklist.data.TasksAPI;

import java.net.HttpURLConnection;
import java.util.Date;

class SyncTask extends AsyncTask<Void, Void, Void> {

    private static final String TAG = SyncTask.class.getSimpleName();
    private final Context mContext;
    private final String mAuthToken;
    private final OnSyncCompleteListener mCallback;
    private boolean mResult = false;
    private int mChanges = 0;
    private Exception mError = null;

    public SyncTask(Context context, String sAuth, OnSyncCompleteListener callback)
    {
        mContext = context;
        mAuthToken = sAuth;
        mCallback = callback;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Sync sync = new Sync(mContext,mAuthToken);

        try {
            mResult = sync.run();
        } catch (TasksAPI.TasksAPIException e) {
            Log.d(TAG,"TasksAPIException " + e.getMessage());
            mError = e;
            mResult = false;

            //If unauthorized clear token so it will try to get a new one
            if(e.getErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                Auth.clearSavedToken(mContext);
        }

        for(int i = 0; i < sync.dbToGoogle.length; i++) {
            mChanges += sync.dbToGoogle[i];
            mChanges += sync.googleToDb[i];
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid)
    {
        Log.d(TAG, "Result=" + mResult + " Changes=" + mChanges);
        if(mResult)
            Prefs.savePrefDate(mContext, Prefs.KEY_LAST_SYNC, new Date());

        mCallback.onSyncFinish(mResult,mError);
    }
}
