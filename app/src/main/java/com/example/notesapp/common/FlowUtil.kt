package com.example.notesapp.common

import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class LiveDataHost<T>(
    private val scope: CoroutineScope,
    initialValue: T
) : MutableLiveData<T>(initialValue) {
    operator fun invoke(flow: Flow<T>) {
        scope.launch {
            flow.collect {
                postValue(it)
            }
        }
    }
}

class LiveDataHostNullable<T>(
    private val scope: CoroutineScope,
    initialValue: T? = null
) : MutableLiveData<T?>(initialValue) {
    operator fun invoke(flow: Flow<T>) {
        scope.launch {
            flow.collect {
                postValue(it)
            }
        }
    }
}
