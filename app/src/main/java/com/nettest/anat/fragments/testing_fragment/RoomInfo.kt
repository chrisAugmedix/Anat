package com.nettest.anat.fragments.testing_fragment

import com.nettest.anat.RoomResult

data class RoomInfo(
    val lteImage: Int, val roomName: String, val totalTimeSeconds: Int,             //RecyclerView Data
    var downloadSpeedResult: Float = 0F, var avgRssi: Int = 0, var avgLink: Int = 0,
    var avgRsrp: Int = 0, val avgRsrq: Int = 0
//                    var connectedAps: List<String>, var wifiRssiHistory: List<SignalStrengthWifi>
                    )

data class SignalStrengthWifi(val strength: Int, val time: Int)

data class wifiObject(val rssi: Int, val link: Int, val bssid: String, val channel: Int, val timestamp: Int)
data class lteObject(val rsrp: Int, val rsrq: Int, val timestamp: Long)