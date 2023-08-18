package com.nettest.anat


import com.nettest.anat.fragments.pc_fragment.EndpointParent
import com.nettest.anat.fragments.testing_fragment.RoomInfo
import com.nettest.anat.fragments.testing_fragment.TestingCellModel

enum class RoomResult(val color: String) {
    GOOD("#228b22"),
    ALERT("#f0e130")
}

//Application Globals
var global_completedPortChecker: Boolean = false

//TESTING ACTIVITY GLOBALS
var global_testStartTimeEpoch: Long = 0
var global_testEndTimeEpoch: Long = 0
var global_testingState = false

var global_testName = ""
var global_roomList: MutableList<RoomInfo> = mutableListOf()

var global_roomSeconds: Int = 0
var global_sessionSeconds: Int = 0
var global_testingRoomTimeLimit: Int = 45

var global_rssiRoomList: MutableList<Int> = mutableListOf()
var global_linkRoomList: MutableList<Int> = mutableListOf()
var global_rsrpRoomList: MutableList<Int> = mutableListOf()
var global_rsrqRoomList: MutableList<Int> = mutableListOf()

var global_testingCellModel: TestingCellModel = TestingCellModel()
var global_downloadResult: Float = 0F


//Testing Activity - App Settings
var global_testingNetworkCadence = 5000L


//PORT CHECKER GLOBALS
var global_resultList = mutableListOf<EndpointParent>()
