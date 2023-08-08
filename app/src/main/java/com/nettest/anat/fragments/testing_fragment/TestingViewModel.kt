package com.nettest.anat.fragments.testing_fragment

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TestingViewModel: ViewModel() {

    private var junk            = MutableLiveData<Int>(0)

    private var totalRooms      = MutableLiveData<Int>(0)
    private var roomSeconds     = MutableLiveData<Int>(0)
    private var sessionSecs     = MutableLiveData<Int>(0)

    private var roomSpeedTestDownload   = MutableLiveData<Float>(0F)
    private var downloadProgress        = MutableLiveData<Int>(0)
    private var roomSpeedTestUpload     = MutableLiveData<Float>(0F)
    private var uploadProgress          = MutableLiveData<Int>(0)

    //WIFI
    private var currentRssi             = MutableLiveData<Int>(0)
    private var currentConnectedApBssid = MutableLiveData<String>("N/A")
    private var currentConnectedSsid    = MutableLiveData<String>("N/A")
    private var avgRssi                 = MutableLiveData<Int>(0)

    //LTE
    private var currentRsrp = MutableLiveData<Int>(0)
    private var currentRsrq = MutableLiveData<Int>(0)
    private var currentBand = MutableLiveData<Int>(0)
    private var avgRsrp     = MutableLiveData<Int>(0)
    private var avgRsrq     = MutableLiveData<Int>(0)

    //Public Functions

    fun roomAddSecond() {
        roomSeconds.value = roomSeconds.value!! + 1
    }

    fun resetRoomSeconds() {
        roomSeconds.value = 0
    }

    fun resetSessionSeconds() {
        sessionSecs.value = 0
        Log.d("startTesting() ", "Resetting Total Seconds\tSeconds: ${sessionSecs.value}")
    }

    fun sessionAddSecond() {
        sessionSecs.value = sessionSecs.value!! + 1
    }


    fun addRoom() {
        totalRooms.value = totalRooms.value!! + 1
    }

    fun resetRooms() {
        totalRooms.value = 0
    }

    fun forceUiChange() {
        if (junk.value != null) {
            junk.value = junk.value!! + 1
            if (junk.value!! > 100) {
                junk.value = 0
            }
        }
    }

    fun setRoomDownloadSpeed(speed: Float) { roomSpeedTestDownload.value = speed }

    fun setRoomUploadSpeed(speed: Float) { roomSpeedTestUpload.value = speed }

    fun setProgressDownload(progress: Int) {
        downloadProgress.value = progress
    }
    fun setProgressUpload(progress: Int) {uploadProgress.value = progress}


    //Getters
    fun getSessionSeconds(): MutableLiveData<Int> {
        return sessionSecs
    }

    fun updateUi(): MutableLiveData<Int> {
        return junk
    }

    fun getRoomSeconds(): MutableLiveData<Int> {
        return roomSeconds
    }

    fun getRoomCount(): MutableLiveData<Int> {
        return totalRooms
    }

    fun getSpeedTestDownload(): MutableLiveData<Float> { return roomSpeedTestDownload }
    fun getSpeedTestUpload(): MutableLiveData<Float> { return roomSpeedTestUpload }

    fun getDownloadProgress(): MutableLiveData<Int> { return downloadProgress }
    fun getUploadProgress(): MutableLiveData<Int> { return uploadProgress }

}