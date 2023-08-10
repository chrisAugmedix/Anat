package com.nettest.anat.fragments.testing_fragment

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nettest.anat.global_linkRoomList
import com.nettest.anat.global_rsrpRoomList
import com.nettest.anat.global_rsrqRoomList
import com.nettest.anat.global_rssiRoomList

class TestingViewModel: ViewModel() {

    //Junk
    private var junk            = MutableLiveData<Int>(0)

    //Session Variables
    private var totalRooms      = MutableLiveData<Int>(0)
    private var sessionSecs     = MutableLiveData<Int>(0)

    //Room Variables
    private var roomSeconds             = MutableLiveData<Int>(0)
    private var downloadProgress        = MutableLiveData<Int>(0)
    private var roomTestingProgressTime = MutableLiveData<Int>(0)

    //WIFI
    private var connectedBssid          = MutableLiveData<String>("N/A")
    private var avgRssi                 = MutableLiveData<Int>(0)
    private var avgLinkRate             = MutableLiveData<Int>(0)

    //LTE
    private var connectedBand   = MutableLiveData<Int>(0)
    private var avgRsrp         = MutableLiveData<Int>(0)
    private var avgRsrq         = MutableLiveData<Int>(0)

    //Public Functions

    fun roomAddSecond() { roomSeconds.value = roomSeconds.value!! + 1 }
    fun resetRoomSeconds() { roomSeconds.value = 0 }
    fun resetSessionSeconds() { sessionSecs.value = 0 }
    fun sessionAddSecond() { sessionSecs.value = sessionSecs.value!! + 1 }
    fun setSessionSeconds(seconds: Int) { sessionSecs.value = seconds }
    fun addSecondToRoomProgressTime() { roomTestingProgressTime.value = roomTestingProgressTime.value!! + 1 }
    fun addRoom() { totalRooms.value = totalRooms.value!! + 1 }
    fun resetRooms() { totalRooms.value = 0 }
    fun resetRoomTestingProgressTime() { roomTestingProgressTime.value = 0 }
    fun forceUiChange() {
        if (junk.value != null) {
            junk.value = junk.value!! + 1
            if (junk.value!! > 100) {
                junk.value = 0
            }
        }
    }

    fun addRssi(rssi: Int) {
        global_rssiRoomList.add(rssi)
        avgRssi.value = global_rssiRoomList.average().toInt()
    }

    fun addLinkRate(rate: Int) {
        global_linkRoomList.add(rate)
        avgLinkRate.value = global_linkRoomList.average().toInt()
    }

    fun addRsrp(rsrp: Int) {
        global_rsrpRoomList.add(rsrp)
        avgRsrp.value = global_rsrpRoomList.average().toInt()
    }

    fun addRsrq(rsrq: Int) {
        global_rsrqRoomList.add(rsrq)
        avgRsrq.value = global_rsrqRoomList.average().toInt()
    }

    fun addConnectedBssid(bssid: String) { connectedBssid.value = bssid }

    fun addConnectedBand(band: Int) { connectedBand.value = band }

    fun resetNetworkStats() {
        avgRssi.value       = 0
        avgLinkRate.value   = 0
        avgRsrp.value       = 0
        avgRsrq.value       = 0
    }


    fun saveRoomData() {
        //Do Nothing For Now
    }


    fun setProgressDownload(progress: Int) { downloadProgress.value = progress }
    //Getters
    fun getSessionSeconds(): MutableLiveData<Int> { return sessionSecs }

    fun updateUi(): MutableLiveData<Int> { return junk }

    fun getRoomSeconds(): MutableLiveData<Int> { return roomSeconds }

    fun getRoomCount(): MutableLiveData<Int> { return totalRooms }

    fun getDownloadProgress(): MutableLiveData<Int> { return downloadProgress }

    fun getRoomTestingProgressTime(): MutableLiveData<Int> { return roomTestingProgressTime }

    fun getRssiAvg(): MutableLiveData<Int> { return avgRssi }
    fun getRateAvg(): MutableLiveData<Int> { return avgLinkRate }
    fun getRsrpAvg(): MutableLiveData<Int> { return avgRsrp }
    fun getRsrqAvg(): MutableLiveData<Int> { return avgRsrq }
    fun getConnectedBand(): MutableLiveData<Int> { return connectedBand }
    fun getConnectedBssid(): MutableLiveData<String> { return connectedBssid }

}
