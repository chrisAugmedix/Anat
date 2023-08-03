package com.nettest.anat


import com.nettest.anat.fragments.pc_fragment.EndpointParent
import com.nettest.anat.fragments.testing_fragment.RoomInfo

enum class RoomResult(val color: String) {
    GOOD("#228b22"),
    ALERT("#f0e130")
}

//TESTING ACTIVITY GLOBALS
var global_testStartTimeEpoch: Long = 0
var global_testEndTimeEpoch: Long = 0
var global_testingState = false
var global_testName = ""
var global_roomList: MutableList<RoomInfo> = mutableListOf()
var global_testingNetworkCadence = 3


//PORT CHECKER GLOBALS
var global_resultList = mutableListOf<EndpointParent>()
