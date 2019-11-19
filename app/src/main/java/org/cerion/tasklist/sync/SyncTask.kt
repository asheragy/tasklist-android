package org.cerion.tasklist.sync


import android.content.Context
import android.os.AsyncTask
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

    override fun onPostExecute(result: Boolean) {
        mCallback.onSyncFinish(result, mError)
    }
}
