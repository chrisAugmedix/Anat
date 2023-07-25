package com.nettest.anat.fragments.testing_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestingViewModel: ViewModel() {

    private var minutes     = MutableLiveData<Int>(0)
    private var seconds     = MutableLiveData<Int>(0)
    private var totalRooms  = MutableLiveData<Int>(0)
    private var totalSecs   = MutableLiveData<Int>(0)
    private var junk        = MutableLiveData<Int>(0)
    private var roomSeconds = MutableLiveData<Int>(0)

    //Public Functions
    fun setTime(min: Int, sec: Int) {
        minutes.value = minutes.value?.plus(min)
        seconds.value = seconds.value?.plus(sec)
    }

    fun roomAddSecond() {
        roomSeconds.value = roomSeconds.value!! + 1
    }

    fun resetRoomSeconds() {
        roomSeconds.value = 0
    }

    fun testingAddSecond() {
        totalSecs.value = totalSecs.value!! + 1
    }

    fun testingResetSeconds() {
        totalSecs.value = 0
    }

    fun addRooms() {
        totalRooms.value = totalRooms.value!! + 1
    }

    fun removeRooms() {
        if (totalRooms.value!! >= 1) totalRooms.value = totalRooms.value!! - 1
    }

    fun forceUiChange() {
        if (junk.value != null) {
            junk.value = junk.value!! + 1
            if (junk.value!! > 100) {
                junk.value = 0
            }
        }
    }


    //Getters
    fun testFuncGetSec(): MutableLiveData<Int> {
        return totalSecs
    }

    fun updateUi(): MutableLiveData<Int> {
        return junk
    }

    fun getRoomSeconds(): MutableLiveData<Int> {
        return roomSeconds
    }

}