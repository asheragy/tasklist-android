package org.cerion.tasklist.sync;


public interface OnSyncCompleteListener {
    void onSyncFinish(boolean bSuccess, Exception e);
}
