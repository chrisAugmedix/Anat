package com.nettest.anat.fragments.testing_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestingViewModel: ViewModel() {

    private var minutes     = MutableLiveData<Int>(0)
    private var seconds     = MutableLiveData<Int>(0)
    private var totalRooms  = MutableLiveData<Int>(0)

    //Public Functions
    fun setTime(min: Int, sec: Int) {
        minutes.value = minutes.value?.plus(min)
        seconds.value = seconds.value?.plus(sec)
    }

    fun addRooms() {
        totalRooms.value = totalRooms.value!! + 1
    }

    fun removeRooms() {
        if (totalRooms.value!! >= 1) totalRooms.value = totalRooms.value!! - 1
    }

}