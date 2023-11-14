package com.nettest.anat


import com.nettest.anat.fragments.pc_fragment.EndpointParent
import com.nettest.anat.fragments.testing_fragment.RoomInfo
import com.nettest.anat.fragments.testing_fragment.TestingCellModel
import com.nettest.anat.fragments.testing_fragment.lteObject

enum class RoomResult(val color: String) {
    GOOD("#228b22"),
    ALERT("#f0e130")
}

//Application Globals
var global_completedPortChecker: Boolean = false

//TESTING ACTIVITY GLOBALS
//Session
var global_testStartTimeEpoch: Long = 0
var global_testEndTimeEpoch: Long = 0
var global_testingState = false
//Room
var global_roomTestingState = false
var global_roomProgressComplete = false

var global_testName = ""
var global_roomList: MutableList<RoomInfo> = mutableListOf()

var global_roomSeconds: Int = 0
var global_testingRoomTimeLimit: Int = 45

var global_testingCellModel: TestingCellModel = TestingCellModel()
var global_downloadResult: Float = 0F

var global_cellTimeLine: MutableList<lteObject> = mutableListOf()

var global_testingMetricsFrequency = 5
var global_portCheckRequired: Boolean = false


//Testing Activity - App Settings
var global_testingNetworkCadence = 5000L


//PORT CHECKER GLOBALS
var global_resultList = mutableListOf<EndpointParent>()
val global_sessionDataList = mutableListOf<SessionData>()


data class SessionData  ( val sessionName: String, val roomDataList: MutableList<RoomData> ,val metricDataList: MutableList<MetricData>, val ssid: String )
//data class RoomData     ( val roomName: String, val metricDataList: MutableList<MetricData>, val roomGrade: RoomGrade, val dlTestResult: Float )
//data class RoomGrade    ( val gradeImage: Int, val gradeColor: Int )
data class MetricData   ( val wifiMetrics: WifiMetrics, val cellMetrics: CellMetrics, val connectivityMetrics: ConnectivityMetrics, val timestamp: Long )
data class WifiMetrics  ( val ip: String, val rssi: Int, val linkRate: Int, val bssid: String, val channel: Int, val neighborList: MutableList<NeighborData> )
data class NeighborData ( val rssi: Int, val bssid: String, val channel: Int )
data class CellMetrics  ( val rssi: Int, val rsrp: Int, val rsrq: Int, val band: Int, val earfcn: Int, val pci: Int )
data class ConnectivityMetrics ( val pingResultList: MutableList<PingResult> )
data class PingResult   ( val destination: String, val duration: Float, val result: Boolean )

class RoomGrade {

    private var image: Int = R.drawable.room_grade_pass
    private var gradeValue = 3

    fun getGrade() = image
    fun failGrade() {
        image = R.drawable.room_grade_fail
        gradeValue = 1
    }
    fun raiseGrade() {

        if (gradeValue == 3) return
        if (gradeValue == 2) R.drawable.room_grade_pass
        else R.drawable.room_grade_alert
        gradeValue++

    }

    fun lowerGrade() {
        if (gradeValue == 1) return
        if (gradeValue == 3) R.drawable.room_grade_alert
        else R.drawable.room_grade_fail
        gradeValue--
    }

}
class RoomData(name: String) {

    private var roomName: String = name
    private val metricDataList: MutableList<MetricData> = mutableListOf()
    var cellGrade: RoomGrade = RoomGrade()
    var wifiGrade: RoomGrade = RoomGrade()

    fun addMetrics(metrics: MetricData) {
        updateGrade(metrics)
        metricDataList.add(metrics)
    }

    fun updateRoomName(name: String) { roomName = name }

    private val rsrpList        = mutableListOf<Int>()
    private val rsrqList        = mutableListOf<Int>()
    private val cellRssiList    = mutableListOf<Int>()
    private val cellbandList    = mutableListOf<Int>()
    private val rssiList        = mutableListOf<Int>()
    private val linkRateList    = mutableListOf<Int>()
    private fun updateGrade(metrics: MetricData) {

        //LTE first
        val cell = metrics.cellMetrics
        rsrpList.add(cell.rsrp)
        rsrqList.add(cell.rsrq)
        cellRssiList.add(cell.rssi)
        cellbandList.add(cell.band)
        let {
            if (cellbandList.isEmpty()) return@let
            when (Utility.mostCommonInList(cellbandList)) {
                66, 4 -> { cellGrade.raiseGrade() }
                5, 13 -> { cellGrade.lowerGrade() }
            }

        }

        when (cellRssiList.average().toInt()) {
            in 0 downTo -73 -> { cellGrade.raiseGrade() }
            in -73 downTo -78 -> { }
            else -> { cellGrade.lowerGrade() }
        }

        val wifi = metrics.wifiMetrics
        rssiList.add(wifi.rssi)
        linkRateList.add(wifi.linkRate)


    }






}