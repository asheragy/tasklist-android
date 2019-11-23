package org.cerion.tasklist.common

import androidx.lifecycle.LiveData


open class NonNullLiveData<T>(value: T) : LiveData<T>(value) {
    override fun getValue(): T {
        return super.getValue()!!
    }
}

class NonNullMutableLiveData<T>(value: T) : NonNullLiveData<T>(value) {
    public override fun setValue(value: T) {
        super.setValue(value)
    }
}
