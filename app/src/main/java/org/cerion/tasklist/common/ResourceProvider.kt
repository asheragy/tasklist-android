package org.cerion.tasklist.common

import android.content.Context


interface ResourceProvider {
    fun getString(resId: Int): String
}

class ResourceProvider_Impl(private val context: Context) : ResourceProvider {

    override fun getString(resId: Int): String {
        return context.getString(resId)
    }
}
