package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nettest.anat.Utility

class TestingViewModel: ViewModel() {

    //12.28 -- NEW
    private var sessionSeconds      = MutableLiveData(0) //AKA Total Seconds
    private var totalRooms          = MutableLiveData(0)
    private var roomSeconds         = MutableLiveData(0)

    // Room Progress Bar Info
    private var roomProgressPercent = MutableLiveData(0)
    private var roomProgressBar     = MutableLiveData(0)
    private var roomProgressMessage = MutableLiveData("")


    //11.7 -- new method -- SESSION VIEW
    private val sessionMap = MutableLiveData(mutableMapOf<String, String>())
    private var totalSeconds: Int = 0
    private var sessionTotalRooms: Int = 0
    private var roomProgressTime = MutableLiveData(0)

    //11.7 -- new method -- ROOM VIEW
    private var sessionRoomSeconds: Int = 0
    private var sessionProgressSeconds: Int = 0
    private val roomMap = MutableLiveData(mutableMapOf<String, String>())

    //ROOM -- Network Data
    private val roomWifiRssiList = mutableListOf<Int>()
    private val roomWifiLinkRateList = mutableListOf<Int>()
    private val roomLteRsrpList = mutableListOf<Int>()
    private val roomLteRsrqList = mutableListOf<Int>()
    private val roomLteRssiList = mutableListOf<Int>()

    //11.7 -- END

    fun addSecondToSession() { sessionSeconds.value = sessionSeconds.value!! + 1 }
    fun addRoomToSession() { totalRooms.value = totalRooms.value!! + 1 }
    fun addSecondToRoom() { roomSeconds.value = roomSeconds.value!! + 1 }
    fun setProgressBarPercent(percent: Int) {
        roomProgressPercent.value = percent
        when(percent) {
            in 0..15 ->  {  roomProgressMessage.value = "Grabbing metrics in room... (${percent}%)" }
            in 16..29 -> {  roomProgressMessage.value = "Please standby until completed... (${percent}%)" }
            in 30..45 -> {  roomProgressMessage.value = "Almost at the halfway mark... (${percent}%)" }
            in 46..70 -> {  roomProgressMessage.value = "Just a few more seconds... (${percent}%)" }
            in 71..99 -> {  roomProgressMessage.value = "Finalizing data... (${percent}%)" }
            in 100..Int.MAX_VALUE -> { roomProgressMessage.value = "Allotted Time Required Completed" }
        }
    }
    fun setProgressBar(progress: Int) { roomProgressBar.value = progress }

    fun getSessionSeconds() = sessionSeconds
    fun getSessionRoomsCount() = totalRooms
    fun getRoomSeconds() = roomSeconds



    fun setRoomProgressTime(progress: Int) {
        roomProgressTime.value = progress
    }
    fun getRoomProgressTime() = roomProgressTime

    fun addRoomData() {

        val viewMap = roomMap.value!!
        sessionRoomSeconds++

        viewMap["sessionRoomTime"] = sessionRoomSeconds.toString()

    }

    fun addDataRoomCount() {
        val viewMap = sessionMap.value!!
        sessionTotalRooms++
        viewMap["sessionTotalRooms"] = sessionTotalRooms.toString()
        sessionMap.value = viewMap
    }

    @SuppressLint("MissingPermission")
    fun addRoomNetworkData(wifi: WifiManager, lte: TelephonyManager) {

        val connectionInfo = wifi.connectionInfo
        val bssid = connectionInfo.bssid?.replace("\"", "") ?: "N.A"
        val wifiRssi = connectionInfo.rssi
        val wifiRate = connectionInfo.linkSpeed

        wifiRssi.let { roomWifiRssiList.add(it) }
        wifiRate.let { roomWifiLinkRateList.add(it) }

        val viewMap = roomMap.value!!
        viewMap["wifiRssiAvg"] = if (roomWifiRssiList.isEmpty()) "N/A" else "${roomWifiRssiList.average().toInt()} dBm"
        viewMap["wifiRateAvg"] = if (roomWifiLinkRateList.isEmpty()) "N/A" else "${roomWifiLinkRateList.average().toInt()} Mbps"
        viewMap["wifiBssid"] = bssid

        val cellInfoLte = lte.allCellInfo.firstOrNull { info -> (info is CellInfoLte) && (info.isRegistered)} as CellInfoLte?
        (cellInfoLte?.cellSignalStrength?.rsrp)?.let { roomLteRsrpList.add(it) }
        (cellInfoLte?.cellSignalStrength?.rsrq)?.let { roomLteRsrqList.add(it) }
        (cellInfoLte?.cellSignalStrength?.rssi)?.let { roomLteRssiList.add(it) }
        val earfcn = cellInfoLte?.cellIdentity?.earfcn ?: "N/A"
        val band = if (cellInfoLte == null) "N/A" else Utility.getCellBand((earfcn) as Int)

        viewMap["lteRsrpAvg"] = if (roomLteRsrpList.isEmpty()) "N/A" else "${roomLteRsrpList.average().toInt()} dBm"
        viewMap["lteRsrqAvg"] = if (roomLteRsrqList.isEmpty()) "N/A" else "${roomLteRsrqList.average().toInt()} dBm"
        viewMap["lteRssiAvg"] = if (roomLteRssiList.isEmpty()) "N/A" else "${roomLteRssiList.average().toInt()} dBm"
        viewMap["lteBand"] = band.toString()

        roomMap.value = viewMap

    }

    fun addRoomProgressSecond() {
        val viewMap = roomMap.value!!
        sessionProgressSeconds++
        viewMap["sessionRoomProgress"] = sessionProgressSeconds.toString()
        roomMap.value = viewMap
    }

    fun resetRoomValues() {

        roomLteRsrqList.clear()
        roomLteRssiList.clear()
        roomLteRsrpList.clear()
        roomWifiLinkRateList.clear()
        roomWifiRssiList.clear()

        val viewMap = roomMap.value!!
        sessionProgressSeconds = 0
        viewMap["sessionRoomProgress"] = sessionProgressSeconds.toString()
        viewMap["speedTestResult"] = "Running Download Test..."
        viewMap["speedTestProgress"] = "0"
        roomMap.value = viewMap
    }

    fun resetTestSessionData() {
        val viewMap = sessionMap.value!!
        totalSeconds = 0
        sessionTotalRooms = 0
        viewMap["sessionTotalTime"] = totalSeconds.toString()
        viewMap["sessionTotalRooms"] = sessionTotalRooms.toString()
        sessionMap.value = viewMap
    }
    fun addSessionRoomDownloadProgress(progress: Int) {
        val viewMap = roomMap.value!!
        viewMap["speedTestProgress"] = progress.toString()
        roomMap.value = viewMap
    }

    fun addSessionRoomDownloadResult(result: String) {
        val viewMap = roomMap.value!!
        viewMap["speedTestResult"] = result
        roomMap.value = viewMap
    }

    fun getSessionData() = sessionMap
    fun getRoomData() = roomMap

    //11.7 -- END new methods



}
