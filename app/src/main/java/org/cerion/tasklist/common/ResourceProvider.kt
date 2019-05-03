package org.cerion.tasklist.common

import android.content.Context
import androidx.annotation.StringRes


interface ResourceProvider {
    fun getString(@StringRes resId: Int): String
    fun getString(@StringRes resId: Int, vararg formatArgs: Any): String
}

class ResourceProvider_Impl(private val context: Context) : ResourceProvider {

    override fun getString(resId: Int, vararg formatArgs: Any): String {
        return context.getString(resId, *formatArgs)
    }

    override fun getString(@StringRes resId: Int): String {
        return context.getString(resId)
    }
}
