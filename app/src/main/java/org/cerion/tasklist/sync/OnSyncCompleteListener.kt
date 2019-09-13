package org.cerion.tasklist.sync


interface OnSyncCompleteListener {
    fun onSyncFinish(success: Boolean, e: Exception?)
}
