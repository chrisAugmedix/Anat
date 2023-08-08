package com.nettest.anat.fragments.testing_fragment

import com.nettest.anat.RoomResult

data class RoomInfo(val image: Int, val roomName: String, val roomState: RoomResult, val totalTimeSeconds: Int,
                    var downloadSpeedResult: Float = 0F, var avgRssi: Int = 0, var avgLink: Int = 0,
//                    var connectedAps: List<String>, var wifiRssiHistory: List<SignalStrengthWifi>
                    )

data class SignalStrengthWifi(val strength: Int, val time: Int)