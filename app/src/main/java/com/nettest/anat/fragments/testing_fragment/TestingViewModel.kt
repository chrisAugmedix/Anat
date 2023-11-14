package com.nettest.anat.fragments.testing_fragment

import android.annotation.SuppressLint
import android.net.wifi.WifiManager
import android.telephony.CellInfoLte
import android.telephony.TelephonyManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.nettest.anat.Utility

class TestingViewModel: ViewModel() {

    //11.7 -- new method -- SESSION VIEW
    private val sessionMap = MutableLiveData(mutableMapOf<String, String>())
    private var totalSeconds: Int = 0
    private var sessionTotalRooms: Int = 0


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

    //11.7 -- new method
    fun addSessionData() {

        val viewMap = sessionMap.value!!

        totalSeconds++

        //Session Info
        viewMap["sessionTotalTime"] = totalSeconds.toString()
        viewMap["sessionTotalRooms"] = sessionTotalRooms.toString()

        sessionMap.value = viewMap

    }

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
