package org.cerion.tasklist.sync;


public interface OnSyncCompleteListener {

    void onAuthError(Exception e); //Unable to verify account or get sync token
    void onSyncFinish(boolean bSuccess, Exception e);

}
